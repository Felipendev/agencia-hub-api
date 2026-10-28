package com.agenciahub.api.application.services.followups;

import com.agenciahub.api.application.services.followups.FollowupModels.*;
import com.agenciahub.api.security.SecurityContextUsers;
import com.agenciahub.api.security.TenantContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class FollowupService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private UUID agency() { return TenantContext.requireAgencyId(); }
    private static String text(String value) { return value==null?"":value.trim(); }
    private static void fail(HttpStatus status,String message) { throw new ResponseStatusException(status,message); }
    private static void version(Followup old,int expected) {
        if(old.version()!=expected) fail(HttpStatus.CONFLICT,"Esta ação foi alterada. Recarregue antes de tentar novamente; suas alterações não foram aplicadas.");
    }
    private String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch(JsonProcessingException ex) { throw new IllegalStateException("Não foi possível registrar o histórico.",ex); }
    }
    private Object read(String value) {
        try { return mapper.readValue(value,Object.class); }
        catch(JsonProcessingException ex) { throw new IllegalStateException("Não foi possível ler o histórico.",ex); }
    }
    private static final String SELECT="SELECT f.*,c.name customer_name,u.name responsible_name FROM commercial_followups f JOIN customers c ON c.id=f.customer_id AND c.agency_id=f.agency_id JOIN users u ON u.id=f.responsible_id AND u.agency_id=f.agency_id ";
    private final RowMapper<Followup> row=(r,n)->new Followup(r.getObject("id",UUID.class),Product.valueOf(r.getString("product")),
            r.getObject("customer_id",UUID.class),r.getString("customer_name"),r.getString("title"),r.getString("notes"),r.getObject("due_on",LocalDate.class),
            r.getObject("responsible_id",UUID.class),r.getString("responsible_name"),State.valueOf(r.getString("state")),r.getString("state_reason"),r.getInt("version"),r.getObject("deleted_at")!=null);
    private Followup get(UUID id,boolean lock,boolean deleted) {
        var rows=jdbc.query(SELECT+"WHERE f.agency_id=? AND f.id=?"+(deleted?"":" AND f.deleted_at IS NULL")+(lock?" FOR UPDATE OF f":""),row,agency(),id);
        if(rows.isEmpty()) fail(HttpStatus.NOT_FOUND,"Ação não encontrada nesta agência.");
        return rows.get(0);
    }
    @Transactional(readOnly=true)
    public Followup get(UUID id) { return get(id,false,false); }
    private Input normalize(Input input) {
        return new Input(input.product(),input.customerId(),text(input.title()),text(input.notes()),input.dueOn(),input.responsibleId(),input.expectedVersion());
    }
    private void responsible(UUID id) {
        var found=jdbc.queryForList("SELECT id FROM users WHERE id=? AND agency_id=? AND active=true AND account_kind IN ('AGENCY_OWNER','SALES_AGENT') FOR KEY SHARE",id,agency());
        if(found.isEmpty()) fail(HttpStatus.BAD_REQUEST,"Selecione um responsável ativo desta agência.");
    }
    private void event(String action,Followup before,Followup after) {
        Map<String,Object> snapshot=new LinkedHashMap<>(); snapshot.put("before",before); snapshot.put("after",after);
        jdbc.update("INSERT INTO commercial_followup_events(id,agency_id,followup_id,customer_id,action,snapshot,author_id) VALUES(?,?,?,?,?,?::jsonb,?)",
                UUID.randomUUID(),agency(),after.id(),after.customerId(),action,json(snapshot),SecurityContextUsers.requireUserId());
    }
    public Followup create(UUID key,Input raw) {
        Input input=normalize(raw);
        if(input.expectedVersion()!=0) fail(HttpStatus.BAD_REQUEST,"Uma ação nova não possui versão anterior.");
        var person=jdbc.queryForList("SELECT id FROM customers WHERE id=? AND agency_id=? FOR UPDATE",input.customerId(),agency());
        if(person.isEmpty()) fail(HttpStatus.BAD_REQUEST,"Selecione uma pessoa desta agência.");
        responsible(input.responsibleId());
        UUID id=UUID.randomUUID();
        int inserted=jdbc.update("""
                INSERT INTO commercial_followups(id,agency_id,customer_id,product,title,notes,due_on,responsible_id,creation_key,creation_payload)
                VALUES(?,?,?,?,?,?,?,?,?,?::jsonb) ON CONFLICT(agency_id,creation_key) DO NOTHING
                """,id,agency(),input.customerId(),input.product().name(),input.title(),input.notes(),input.dueOn(),input.responsibleId(),key,json(input));
        if(inserted==0) {
            var old=jdbc.queryForMap("SELECT id,creation_payload::text payload,deleted_at FROM commercial_followups WHERE agency_id=? AND creation_key=?",agency(),key);
            if(old.get("deleted_at")!=null || !read((String)old.get("payload")).equals(read(json(input))))
                fail(HttpStatus.CONFLICT,"Esta confirmação já foi usada. Reabra o formulário para criar outra ação.");
            return get((UUID)old.get("id"));
        }
        Followup saved=get(id); event("CREATED",null,saved); return saved;
    }
    public Followup update(UUID id,Input raw) {
        Followup old=get(id,true,false); version(old,raw.expectedVersion()); Input input=normalize(raw);
        if(old.state()!=State.OPEN) fail(HttpStatus.CONFLICT,"Reabra a ação antes de editar ou reprogramar.");
        if(old.product()!=input.product() || !old.customerId().equals(input.customerId()))
            fail(HttpStatus.BAD_REQUEST,"Pessoa e produto são preservados nesta ação.");
        responsible(input.responsibleId());
        jdbc.update("UPDATE commercial_followups SET title=?,notes=?,due_on=?,responsible_id=?,version=version+1,updated_at=now() WHERE agency_id=? AND id=?",
                input.title(),input.notes(),input.dueOn(),input.responsibleId(),agency(),id);
        Followup saved=get(id); event(old.dueOn().equals(saved.dueOn())?"UPDATED":"RESCHEDULED",old,saved); return saved;
    }
    public Followup transition(UUID id,Transition input) {
        Followup old=get(id,true,false); version(old,input.expectedVersion());
        if(old.state()==input.state()) return old;
        if(old.state()!=State.OPEN && input.state()!=State.OPEN) fail(HttpStatus.CONFLICT,"Reabra a ação antes de mudar seu encerramento.");
        if(input.state()!=State.DONE && text(input.reason()).isEmpty()) fail(HttpStatus.BAD_REQUEST,"Informe o motivo do cancelamento ou da reabertura.");
        jdbc.update("UPDATE commercial_followups SET state=?,state_reason=?,version=version+1,updated_at=now() WHERE agency_id=? AND id=?",
                input.state().name(),text(input.reason()),agency(),id);
        Followup saved=get(id); event(input.state()==State.OPEN?"REOPENED":input.state().name(),old,saved); return saved;
    }
    public void delete(UUID id,int expected) {
        Followup old=get(id,true,false); version(old,expected);
        if(old.state()!=State.OPEN) fail(HttpStatus.CONFLICT,"Ações encerradas preservam histórico. Reabra para excluir, ou mantenha o cancelamento.");
        jdbc.update("UPDATE commercial_followups SET deleted_at=now(),version=version+1,updated_at=now() WHERE agency_id=? AND id=?",agency(),id);
        event("DELETED",old,get(id,false,true));
    }
    @Transactional(readOnly=true)
    public Board list(Product product,UUID customer,UUID responsible,State state,Bucket bucket,boolean deleted) {
        LocalDate today=LocalDate.now(ZoneId.of("America/Fortaleza"));
        String scope=" WHERE f.agency_id=? AND f.product=? AND f.deleted_at IS NULL";
        List<Object> args=new ArrayList<>(List.of(agency(),product.name()));
        if(customer!=null){scope+=" AND f.customer_id=?";args.add(customer);}
        if(responsible!=null){scope+=" AND f.responsible_id=?";args.add(responsible);}
        List<Object> countArgs=new ArrayList<>(List.of(today,today,today)); countArgs.addAll(args);
        Counts counts=jdbc.queryForObject("SELECT count(*) FILTER(WHERE due_on<?),count(*) FILTER(WHERE due_on=?),count(*) FILTER(WHERE due_on>?) FROM commercial_followups f"+scope+" AND f.state='OPEN'",
                (r,n)->new Counts(r.getLong(1),r.getLong(2),r.getLong(3)),countArgs.toArray());
        if(deleted) scope=scope.replace("f.deleted_at IS NULL","f.deleted_at IS NOT NULL");
        if(state!=null){scope+=" AND f.state=?";args.add(state.name());}
        if(bucket!=Bucket.ALL){scope+=" AND f.due_on"+(bucket==Bucket.OVERDUE?"<":bucket==Bucket.TODAY?"=":">")+"?";args.add(today);}
        return new Board(today,counts,jdbc.query(SELECT+scope+" ORDER BY f.due_on,f.id",row,args.toArray()));
    }
    @Transactional(readOnly=true)
    public List<Event> history(UUID id) {
        var result=jdbc.query("""
                SELECT e.*,u.name author_name FROM commercial_followup_events e LEFT JOIN users u ON u.id=e.author_id AND u.agency_id=e.agency_id
                WHERE e.agency_id=? AND e.followup_id=? ORDER BY e.created_at,e.id
                """,(r,n)->new Event(r.getObject("id",UUID.class),r.getString("action"),read(r.getString("snapshot")),r.getObject("author_id",UUID.class),r.getString("author_name"),r.getObject("created_at",OffsetDateTime.class)),agency(),id);
        if(result.isEmpty()) fail(HttpStatus.NOT_FOUND,"Histórico não encontrado nesta agência.");
        return result;
    }
}
