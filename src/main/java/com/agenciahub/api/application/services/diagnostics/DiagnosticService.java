package com.agenciahub.api.application.services.diagnostics;

import com.agenciahub.api.application.services.MilesWalletService;
import com.agenciahub.api.domain.MilesWallet;
import com.agenciahub.api.security.SecurityContextUsers;
import com.agenciahub.api.security.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.OffsetDateTime;
import java.util.*;
import static com.agenciahub.api.application.services.diagnostics.DiagnosticDtos.*;
import static org.springframework.http.HttpStatus.*;

@Service @RequiredArgsConstructor
public class DiagnosticService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final MilesWalletService wallet;
    private UUID agency(){return TenantContext.requireAgencyId();}
    private void fail(String message){throw new ResponseStatusException(CONFLICT,message);}
    private void version(int actual,int expected){if(actual!=expected)fail("O diagnóstico foi alterado. Recarregue antes de confirmar; seu formulário foi preservado.");}
    private String json(Object o){try{return mapper.writeValueAsString(o);}catch(Exception e){throw new IllegalStateException("Não foi possível salvar o diagnóstico.",e);}}
    private <T>T read(String s,Class<T> c){try{return mapper.readValue(s,c);}catch(Exception e){throw new IllegalStateException("Não foi possível consultar o diagnóstico.",e);}}
    @Transactional(readOnly=true)
    public Config config(){return configs(agency(),false).orElse(null);}
    private Optional<Config> configs(UUID agency,boolean lock){return jdbc.query("SELECT * FROM miles_diagnostic_configs WHERE agency_id=?"+(lock?" FOR UPDATE":""),(r,n)->new Config(r.getObject("token",UUID.class),r.getString("title"),r.getString("introduction"),r.getBoolean("enabled"),r.getInt("version")),agency).stream().findFirst();}
    @Transactional
    public Config configure(ConfigInput in){
        jdbc.queryForList("SELECT id FROM agencies WHERE id=? FOR UPDATE",agency());
        Config before=configs(agency(),true).orElse(null);version(before==null?0:before.version(),in.expectedVersion());
        if(before==null)jdbc.update("INSERT INTO miles_diagnostic_configs(agency_id,token,title,introduction,enabled) VALUES(?,?,?,?,?)",agency(),UUID.randomUUID(),in.title().trim(),in.introduction().trim(),in.enabled());
        else jdbc.update("UPDATE miles_diagnostic_configs SET title=?,introduction=?,enabled=?,version=version+1 WHERE agency_id=?",in.title().trim(),in.introduction().trim(),in.enabled(),agency());
        return config();
    }
    @Transactional
    public Config revoke(int expected){
        Config before=configs(agency(),true).orElseThrow(()->new ResponseStatusException(NOT_FOUND,"Link não encontrado."));version(before.version(),expected);
        jdbc.update("UPDATE miles_diagnostic_configs SET token=?,enabled=false,version=version+1 WHERE agency_id=?",UUID.randomUUID(),agency());return config();
    }
    private UUID publicAgency(UUID token,boolean lock){return jdbc.query("SELECT c.agency_id FROM miles_diagnostic_configs c JOIN agencies a ON a.id=c.agency_id WHERE c.token=? AND c.enabled AND a.status='ACTIVE'"+(lock?" FOR UPDATE OF c":""),(r,n)->r.getObject(1,UUID.class),token).stream().findFirst().orElseThrow(()->new ResponseStatusException(NOT_FOUND,"Este formulário está indisponível."));}
    @Transactional(readOnly=true)
    public PublicConfig publicConfig(UUID token){UUID id=publicAgency(token,false);Config c=configs(id,false).orElseThrow();return new PublicConfig(jdbc.queryForObject("SELECT name FROM agencies WHERE id=?",String.class,id),c.title(),c.introduction(),CONSENT);}
    private Data clean(Data in){
        for(var item:in.items()){if(item.kind()==MilesWallet.Kind.EXPIRY)throw new IllegalArgumentException("Vencimentos são cadastrados pela agência na conta revisada.");item.data().validate(item.kind());}
        return new Data(in.name().trim(),in.email().trim().toLowerCase(Locale.ROOT),in.phone().trim(),in.profile(),List.copyOf(in.items()));
    }
    @Transactional
    public void submit(UUID token,UUID key,Submission input){
        UUID id=publicAgency(token,true);Data data=clean(input.data());
        var old=jdbc.query("SELECT original_data FROM miles_diagnostics WHERE agency_id=? AND request_key=?",(r,n)->read(r.getString(1),Data.class),id,key);
        if(!old.isEmpty()){if(!json(old.get(0)).equals(json(data)))fail("Este envio já foi recebido com outros dados. Revise antes de iniciar outro envio.");return;}
        Long count=jdbc.queryForObject("SELECT count(*) FROM miles_diagnostics WHERE agency_id=? AND source='PUBLIC' AND created_at>now()-interval '1 hour'",Long.class,id);
        if(count>=60)throw new ResponseStatusException(TOO_MANY_REQUESTS,"O formulário atingiu o limite de envios. Tente mais tarde.");
        create(id,key,data,"PUBLIC",CONSENT,null);
    }
    private Entry create(UUID agency,UUID key,Data data,String source,String consent,UUID author){
        UUID id=UUID.randomUUID();jdbc.update("INSERT INTO miles_diagnostics(id,agency_id,request_key,original_data,data,source,consent_version) VALUES(?,?,?,?::jsonb,?::jsonb,?,?)",id,agency,key,json(data),json(data),source,consent);
        Entry entry=entry(agency,id,false);event(agency,entry,"CREATED",author,"");return entry;
    }
    @Transactional
    public Entry createStaff(UUID key,Data input){
        jdbc.queryForList("SELECT id FROM agencies WHERE id=? FOR UPDATE",agency());Data data=clean(input);
        var old=jdbc.query("SELECT id,original_data FROM miles_diagnostics WHERE agency_id=? AND request_key=?",(r,n)->Map.entry(r.getObject(1,UUID.class),read(r.getString(2),Data.class)),agency(),key);
        if(!old.isEmpty()){if(!json(old.get(0).getValue()).equals(json(data)))fail("A confirmação já foi usada em outro cadastro.");return entry(agency(),old.get(0).getKey(),false);}
        return create(agency(),key,data,"STAFF",null,SecurityContextUsers.requireUserId());
    }
    @Transactional(readOnly=true)
    public List<Summary> list(){return jdbc.query("SELECT id,data,version,archived,receipt IS NOT NULL incorporated,created_at FROM miles_diagnostics WHERE agency_id=? ORDER BY created_at DESC,id",(r,n)->{Data d=read(r.getString("data"),Data.class);return new Summary(r.getObject("id",UUID.class),d.name(),d.email(),r.getInt("version"),r.getBoolean("archived"),r.getBoolean("incorporated"),r.getObject("created_at",OffsetDateTime.class));},agency());}
    @Transactional(readOnly=true)
    public Entry get(UUID id){return entry(agency(),id,false);}
    private Entry entry(UUID agency,UUID id,boolean lock){return jdbc.query("SELECT * FROM miles_diagnostics WHERE id=? AND agency_id=?"+(lock?" FOR UPDATE":""),(r,n)->new Entry(id,read(r.getString("original_data"),Data.class),read(r.getString("data"),Data.class),r.getString("source"),r.getString("consent_version"),r.getInt("version"),r.getBoolean("archived"),r.getObject("customer_id",UUID.class),r.getString("receipt")==null?null:read(r.getString("receipt"),Receipt.class),r.getObject("created_at",OffsetDateTime.class)),id,agency).stream().findFirst().orElseThrow(()->new ResponseStatusException(NOT_FOUND,"Diagnóstico não encontrado."));}
    @Transactional
    public Entry edit(UUID id,Edit in){Entry old=entry(agency(),id,true);version(old.version(),in.expectedVersion());editable(old);Data data=clean(in.data());jdbc.update("UPDATE miles_diagnostics SET data=?::jsonb,version=version+1,updated_at=now() WHERE id=? AND agency_id=?",json(data),id,agency());Entry updated=get(id);event(agency(),updated,"UPDATED",SecurityContextUsers.requireUserId(),"");return updated;}
    private void editable(Entry e){if(e.archived())fail("Reative o diagnóstico antes de editar ou incorporar.");if(e.receipt()!=null)fail("Este diagnóstico já foi incorporado. Ajuste os dados pelo cadastro da carteira; o comprovante é histórico.");}
    @Transactional
    public Entry archive(UUID id,State in,boolean archived){Entry old=entry(agency(),id,true);version(old.version(),in.expectedVersion());jdbc.update("UPDATE miles_diagnostics SET archived=?,version=version+1,updated_at=now() WHERE id=? AND agency_id=?",archived,id,agency());Entry updated=get(id);event(agency(),updated,archived?"ARCHIVED":"RESTORED",SecurityContextUsers.requireUserId(),in.reason().trim());return updated;}
    @Transactional
    public Receipt incorporate(UUID id,Apply in){
        Entry old=entry(agency(),id,true);
        if(old.receipt()!=null){String prior=jdbc.queryForObject("SELECT applied_input::text FROM miles_diagnostics WHERE id=? AND agency_id=?",String.class,id,agency());if(!read(prior,Apply.class).equals(in))fail("Este diagnóstico já foi incorporado com outra confirmação.");return old.receipt();}
        version(old.version(),in.expectedVersion());editable(old);
        if(new HashSet<>(in.items()).size()!=in.items().size()||in.items().stream().anyMatch(i->i>=old.data().items().size())||(!in.importProfile()&&in.items().isEmpty()))throw new IllegalArgumentException("Selecione o perfil ou registros válidos, sem repetição.");
        if(jdbc.queryForList("SELECT id FROM customers WHERE id=? AND agency_id=? FOR UPDATE",in.customerId(),agency()).isEmpty())throw new ResponseStatusException(NOT_FOUND,"Pessoa não encontrada nesta agência.");
        String author=SecurityContextUsers.requireUserId().toString();
        if(in.importProfile()){
            var current=wallet.view(in.customerId()).profile();
            if((current==null?0:current.version())!=in.profileVersion())fail("O perfil da carteira foi alterado. Recarregue e revise a substituição antes de confirmar.");
            wallet.saveProfile(in.customerId(),new MilesWallet.ProfileInput(old.data().profile(),in.profileVersion()),author);
        }
        List<UUID> ids=new ArrayList<>();for(int index:in.items()){var item=old.data().items().get(index);UUID key=UUID.randomUUID();wallet.create(in.customerId(),key,new MilesWallet.ItemInput(item.kind(),item.data(),0),author);ids.add(key);}
        Receipt receipt=new Receipt(in.customerId(),SecurityContextUsers.requireUserId(),OffsetDateTime.now(java.time.ZoneOffset.UTC),in.importProfile(),ids,List.copyOf(in.items()));
        jdbc.update("UPDATE miles_diagnostics SET customer_id=?,applied_input=?::jsonb,receipt=?::jsonb,version=version+1,updated_at=now() WHERE id=? AND agency_id=?",in.customerId(),json(in),json(receipt),id,agency());event(agency(),get(id),"INCORPORATED",SecurityContextUsers.requireUserId(),"");return receipt;
    }
    private void event(UUID agency,Entry entry,String action,UUID author,String reason){jdbc.update("INSERT INTO miles_diagnostic_events(id,agency_id,diagnostic_id,action,author_id,reason,snapshot) VALUES(?,?,?,?,?,?,?::jsonb)",UUID.randomUUID(),agency,entry.id(),action,author,reason,json(entry));}
    @Transactional(readOnly=true)
    public List<Event> events(UUID id){get(id);return jdbc.query("SELECT * FROM miles_diagnostic_events WHERE agency_id=? AND diagnostic_id=? ORDER BY created_at,id",(r,n)->new Event(r.getString("action"),r.getObject("author_id",UUID.class),r.getString("reason"),read(r.getString("snapshot"),Object.class),r.getObject("created_at",OffsetDateTime.class)),agency(),id);}
}

