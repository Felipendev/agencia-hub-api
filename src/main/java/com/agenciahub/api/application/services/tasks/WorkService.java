package com.agenciahub.api.application.services.tasks;

import com.agenciahub.api.application.services.tasks.WorkModels.*;
import com.agenciahub.api.security.SecurityContextUsers;
import com.agenciahub.api.security.TenantContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/** Persisted work for TRAVEL/MILES. No writes to commercial, finance or wallet tables. */
@Service
@RequiredArgsConstructor
@Transactional
public class WorkService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private UUID agency() { return TenantContext.requireAgencyId(); }
    private UUID author() { return SecurityContextUsers.requireUserId(); }
    private static String text(String value) { return value == null ? "" : value.trim(); }
    private static void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static void conflict(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT, message); }
    private static void version(int actual, int expected) {
        if (actual != expected) conflict("Este registro foi alterado. Recarregue antes de tentar novamente.");
    }
    private String json(Object object) {
        try { return mapper.writeValueAsString(object); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Não foi possível registrar a operação.", e); }
    }
    private <T> T read(String value, Class<T> type) {
        try { return mapper.readValue(value, type); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Não foi possível consultar o registro.", e); }
    }
    private List<Step> steps(String value) {
        try { return mapper.readValue(value, new TypeReference<>() {}); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Modelo inválido.", e); }
    }
    private <T> T one(List<T> rows) {
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro não encontrado nesta agência.");
        return rows.get(0);
    }
    private void event(String type, UUID id, UUID customer, String action, Object snapshot) {
        jdbc.update("INSERT INTO work_task_events(id,agency_id,resource_type,resource_id,customer_id,action,snapshot,author_id) VALUES(?,?,?,?,?,?,?::jsonb,?)",
                UUID.randomUUID(), agency(), type, id, customer, action, json(snapshot), author());
    }
    private void taskEvents(List<Task> tasks, String action) {
        UUID tenant=agency(),user=author();
        List<Object[]> rows=tasks.stream().map(task->new Object[]{UUID.randomUUID(),tenant,task.id(),task.customerId(),action,json(task),user}).toList();
        if(!rows.isEmpty()) jdbc.batchUpdate("INSERT INTO work_task_events(id,agency_id,resource_type,resource_id,customer_id,action,snapshot,author_id) VALUES(?,?,'TASK',?,?,?,?::jsonb,?)",rows);
    }
    private final RowMapper<Task> taskMapper = (r,n) -> new Task(r.getObject("id",UUID.class), Product.valueOf(r.getString("product")),
            r.getObject("customer_id",UUID.class), r.getString("customer_name"),r.getString("title"),r.getString("notes"),
            r.getObject("due_on",LocalDate.class),r.getObject("responsible_id",UUID.class),r.getString("responsible_name"),
            r.getObject("trip_id",UUID.class),r.getObject("quotation_id",UUID.class),r.getObject("run_id",UUID.class),State.valueOf(r.getString("state")),
            r.getInt("version"),r.getObject("state_by",UUID.class),r.getObject("state_at",OffsetDateTime.class),r.getString("state_reason"),r.getObject("deleted_at")!=null);
    private static final String TASK_SELECT = "SELECT t.*, c.name customer_name, u.name responsible_name FROM work_tasks t JOIN customers c ON c.id=t.customer_id AND c.agency_id=t.agency_id LEFT JOIN users u ON u.id=t.responsible_id AND u.agency_id=t.agency_id ";
    private RowMapper<Template> templateMapper() {
        return (r,n) -> new Template(r.getObject("id",UUID.class),Product.valueOf(r.getString("product")),r.getString("name"),
                Frequency.valueOf(r.getString("frequency")),steps(r.getString("steps")),r.getInt("version"),r.getObject("deleted_at")!=null);
    }
    private RowMapper<Run> runMapper() {
        return (r,n) -> new Run(r.getObject("id",UUID.class),Product.valueOf(r.getString("product")),r.getObject("customer_id",UUID.class),r.getString("customer_name"),
                r.getObject("template_id",UUID.class),r.getString("title"),r.getString("notes"),r.getString("period_key"),
                r.getObject("period_start",LocalDate.class),r.getObject("period_end",LocalDate.class),read(r.getString("snapshot"),Template.class),r.getInt("version"),r.getObject("cancelled_at",OffsetDateTime.class),r.getString("cancel_reason"));
    }
    private static final String RUN_SELECT = "SELECT r.*,c.name customer_name FROM checklist_runs r JOIN customers c ON c.id=r.customer_id AND c.agency_id=r.agency_id ";

    @Transactional(readOnly=true)
    public List<Member> members() {
        return jdbc.query("SELECT id,name FROM users WHERE agency_id=? AND active=true AND account_kind IN ('AGENCY_OWNER','SALES_AGENT') ORDER BY name,id",
                (r,n)->new Member(r.getObject("id",UUID.class),r.getString("name")),agency());
    }
    private void customer(UUID id) {
        if (!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM customers WHERE id=? AND agency_id=?)",Boolean.class,id,agency())))
            bad("Selecione uma pessoa desta agência.");
    }
    @Transactional(readOnly=true)
    public References references(UUID customerId) {
        customer(customerId);
        RowMapper<Member> row=(r,n)->new Member(r.getObject("id",UUID.class),r.getString("name"));
        return new References(jdbc.query("SELECT id,coalesce(booking_locator,service_type)||' · '||left(id::text,8) name FROM trips WHERE agency_id=? AND customer_id=? ORDER BY id",row,agency(),customerId),
                jdbc.query("SELECT id,title||' · '||left(id::text,8) name FROM quotations WHERE agency_id=? AND customer_id=? ORDER BY id",row,agency(),customerId));
    }
    private void responsible(UUID id) {
        if (id != null && !Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM users WHERE id=? AND agency_id=? AND active=true AND account_kind IN ('AGENCY_OWNER','SALES_AGENT'))",Boolean.class,id,agency())))
            bad("O responsável deve ser membro ativo desta agência.");
    }
    private void links(TaskInput input) {
        customer(input.customerId()); responsible(input.responsibleId());
        if (input.product()==Product.MILES && (input.tripId()!=null || input.quotationId()!=null)) bad("Vínculos de viagem e cotação pertencem ao produto Viagens.");
        if (input.tripId()!=null && !Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM trips WHERE id=? AND agency_id=? AND customer_id=?)",Boolean.class,input.tripId(),agency(),input.customerId()))) bad("A viagem deve pertencer à pessoa e agência selecionadas.");
        if (input.quotationId()!=null && !Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM quotations WHERE id=? AND agency_id=? AND customer_id=?)",Boolean.class,input.quotationId(),agency(),input.customerId()))) bad("A cotação deve pertencer à pessoa e agência selecionadas.");
    }
    @Transactional(readOnly=true)
    public List<Task> tasks(Product product, UUID customerId, UUID responsibleId, State state, LocalDate dueBefore, UUID runId) {
        StringBuilder sql=new StringBuilder(TASK_SELECT+"WHERE t.agency_id=? AND t.deleted_at IS NULL");
        List<Object> args=new ArrayList<>(); args.add(agency());
        if(product!=null){sql.append(" AND t.product=?");args.add(product.name());}
        if(customerId!=null){sql.append(" AND t.customer_id=?");args.add(customerId);}
        if(responsibleId!=null){sql.append(" AND t.responsible_id=?");args.add(responsibleId);}
        if(state!=null){sql.append(" AND t.state=?");args.add(state.name());}
        if(dueBefore!=null){sql.append(" AND t.due_on<?");args.add(dueBefore);}
        if(runId!=null){sql.append(" AND t.run_id=?");args.add(runId);}
        sql.append(" ORDER BY t.due_on,t.id");
        return jdbc.query(sql.toString(),taskMapper,args.toArray());
    }
    public Task task(UUID id) { return task(id,false,false); }
    private Task task(UUID id, boolean lock, boolean deleted) {
        return one(jdbc.query(TASK_SELECT+"WHERE t.id=? AND t.agency_id=?"+(deleted?"":" AND t.deleted_at IS NULL")+(lock?" FOR UPDATE OF t":""),taskMapper,id,agency()));
    }
    public Task create(UUID key, TaskInput input) { return create(key,input,null); }
    private Task create(UUID key, TaskInput input, UUID runId) {
        links(input);
        if(input.expectedVersion()!=0) bad("Uma tarefa nova deve iniciar sem versão anterior.");
        UUID id=UUID.randomUUID();
        int count=jdbc.update("""
                INSERT INTO work_tasks(id,agency_id,customer_id,product,title,notes,due_on,responsible_id,trip_id,quotation_id,run_id,creation_key,creation_payload)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?::jsonb) ON CONFLICT(agency_id,creation_key) DO NOTHING
                """,id,agency(),input.customerId(),input.product().name(),text(input.title()),text(input.notes()),input.dueOn(),input.responsibleId(),input.tripId(),input.quotationId(),runId,key,json(input));
        if(count==0) {
            var rows=jdbc.queryForList("SELECT id,creation_payload::text payload,deleted_at FROM work_tasks WHERE agency_id=? AND creation_key=?",agency(),key);
            var old=one(rows);
            if(old.get("deleted_at")!=null || !read((String)old.get("payload"),Object.class).equals(read(json(input),Object.class))) conflict("Esta confirmação já foi usada em outra criação. Reabra o formulário para uma nova tarefa.");
            return task((UUID)old.get("id"));
        }
        Task saved=task(id); event("TASK",id,saved.customerId(),"CREATED",saved); return saved;
    }
    public Task update(UUID id, TaskInput input) {
        Task old=task(id,true,false); version(old.version(),input.expectedVersion());
        if(old.state()!=State.OPEN) conflict("Reabra a tarefa antes de editar.");
        if(old.product()!=input.product()) bad("O produto da tarefa é preservado. Crie outra tarefa para o outro produto.");
        if(old.runId()!=null && (old.product()!=input.product() || !old.customerId().equals(input.customerId()))) bad("Pessoa e produto da execução são preservados.");
        links(input);
        jdbc.update("UPDATE work_tasks SET product=?,customer_id=?,title=?,notes=?,due_on=?,responsible_id=?,trip_id=?,quotation_id=?,version=version+1,updated_at=now() WHERE id=? AND agency_id=?",
                input.product().name(),input.customerId(),text(input.title()),text(input.notes()),input.dueOn(),input.responsibleId(),input.tripId(),input.quotationId(),id,agency());
        Task saved=task(id); event("TASK",id,saved.customerId(),"UPDATED",saved);
        // Retain the previous customer's review link as well when a task is moved.
        if(!old.customerId().equals(saved.customerId())) event("TASK",id,old.customerId(),"MOVED_FROM",old);
        return saved;
    }
    public Task transition(UUID id, Transition input) {
        Task initial=task(id,false,false);
        Run execution=initial.runId()==null?null:run(initial.runId(),true);
        Task old=task(id,true,false); version(old.version(),input.expectedVersion());
        if(execution!=null && input.state()==State.OPEN && execution.cancelledAt()!=null) conflict("A execução foi cancelada e não permite reabrir suas tarefas.");
        if(old.state()==input.state()) return old;
        if(old.state()!=State.OPEN && input.state()!=State.OPEN) conflict("Reabra a tarefa antes de mudar sua conclusão ou cancelamento.");
        if(input.state()==State.CANCELLED && text(input.reason()).isEmpty()) bad("Informe o motivo do cancelamento.");
        jdbc.update("UPDATE work_tasks SET state=?,state_by=?,state_at=now(),state_reason=?,version=version+1,updated_at=now() WHERE id=? AND agency_id=?",
                input.state().name(),author(),text(input.reason()),id,agency());
        Task saved=task(id); event("TASK",id,saved.customerId(),input.state().name(),saved);return saved;
    }
    public void deleteTask(UUID id,int expected) {
        Task old=task(id,true,false); version(old.version(),expected);
        if(old.state()!=State.OPEN) conflict("Tarefas encerradas preservam histórico. Reabra antes de excluir, ou mantenha o cancelamento.");
        jdbc.update("UPDATE work_tasks SET deleted_at=now(),version=version+1,updated_at=now() WHERE id=? AND agency_id=?",id,agency());
        event("TASK",id,old.customerId(),"DELETED",task(id,false,true));
    }
    @Transactional(readOnly=true)
    public List<Template> templates(Product product) {
        return product==null?jdbc.query("SELECT * FROM checklist_templates WHERE agency_id=? AND deleted_at IS NULL ORDER BY name,id",templateMapper(),agency()):
                jdbc.query("SELECT * FROM checklist_templates WHERE agency_id=? AND product=? AND deleted_at IS NULL ORDER BY name,id",templateMapper(),agency(),product.name());
    }
    private Template template(UUID id,boolean lock) {
        return one(jdbc.query("SELECT * FROM checklist_templates WHERE id=? AND agency_id=? AND deleted_at IS NULL"+(lock?" FOR UPDATE":""),templateMapper(),id,agency()));
    }
    public Template saveTemplate(UUID id,TemplateInput input) {
        if(id==null){
            if(input.expectedVersion()!=0)bad("Modelo novo não possui versão anterior.");
            id=UUID.randomUUID();
            jdbc.update("INSERT INTO checklist_templates(id,agency_id,product,name,frequency,steps) VALUES(?,?,?,?,?,?::jsonb)",id,agency(),input.product().name(),text(input.name()),input.frequency().name(),json(input.steps()));
        }else{
            Template old=template(id,true);version(old.version(),input.expectedVersion());
            if(old.product()!=input.product())bad("O produto do modelo não pode mudar; crie outro modelo para o outro produto.");
            jdbc.update("UPDATE checklist_templates SET name=?,frequency=?,steps=?::jsonb,version=version+1,updated_at=now() WHERE id=? AND agency_id=?",text(input.name()),input.frequency().name(),json(input.steps()),id,agency());
        }
        Template saved=template(id,false);event("TEMPLATE",id,null,saved.version()==1?"CREATED":"UPDATED",saved);return saved;
    }
    public void deleteTemplate(UUID id,int expected){
        Template old=template(id,true);version(old.version(),expected);
        jdbc.update("UPDATE checklist_templates SET deleted_at=now(),version=version+1 WHERE id=? AND agency_id=?",id,agency());
        event("TEMPLATE",id,null,"DELETED",old);
    }
    public Run apply(UUID id,ApplyInput input){
        Template model=template(id,true);customer(input.customerId());responsible(input.responsibleId());
        LocalDate start=input.date(),end=input.date();
        if(model.frequency()==Frequency.WEEKLY){start=start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));end=start.plusDays(6);}
        if(model.frequency()==Frequency.MONTHLY){start=start.withDayOfMonth(1);end=start.with(TemporalAdjusters.lastDayOfMonth());}
        String key=model.frequency()==Frequency.ONCE?"ONCE":(model.frequency()==Frequency.WEEKLY?"W:":"M:")+start;
        List<UUID> prior=jdbc.query("SELECT id FROM checklist_runs WHERE agency_id=? AND template_id=? AND customer_id=? AND period_key=?",(r,n)->r.getObject(1,UUID.class),agency(),id,input.customerId(),key);
        if(!prior.isEmpty())return run(prior.get(0),false);
        UUID runId=UUID.randomUUID();
        jdbc.update("INSERT INTO checklist_runs(id,agency_id,template_id,customer_id,product,title,period_key,period_start,period_end,snapshot) VALUES(?,?,?,?,?,?,?,?,?,?::jsonb)",runId,agency(),id,input.customerId(),model.product().name(),model.name(),key,start,end,json(model));
        List<Object[]> generated=new ArrayList<>();
        for(Step step:model.steps()) {
            TaskInput task=new TaskInput(model.product(),input.customerId(),step.title(),"",start.plusDays(step.dueOffsetDays()),input.responsibleId(),null,null,0);
            generated.add(new Object[]{UUID.randomUUID(),agency(),input.customerId(),model.product().name(),text(step.title()),task.dueOn(),input.responsibleId(),runId,UUID.randomUUID(),json(task)});
        }
        jdbc.batchUpdate("INSERT INTO work_tasks(id,agency_id,customer_id,product,title,due_on,responsible_id,run_id,creation_key,creation_payload) VALUES(?,?,?,?,?,?,?,?,?,?::jsonb)",generated);
        taskEvents(tasks(null,null,null,null,null,runId),"CREATED");
        Run saved=run(runId,false);event("RUN",runId,input.customerId(),"CREATED",saved);return saved;
    }
    @Transactional(readOnly=true)
    public List<Run> runs(Product product,UUID customerId){
        String sql=RUN_SELECT+"WHERE r.agency_id=?";
        List<Object> args=new ArrayList<>();args.add(agency());
        if(product!=null){sql+=" AND r.product=?";args.add(product.name());}
        if(customerId!=null){sql+=" AND r.customer_id=?";args.add(customerId);}
        return jdbc.query(sql+" ORDER BY r.period_start DESC,r.id",runMapper(),args.toArray());
    }
    public Run run(UUID id,boolean lock){return one(jdbc.query(RUN_SELECT+"WHERE r.id=? AND r.agency_id=?"+(lock?" FOR UPDATE OF r":""),runMapper(),id,agency()));}
    public Run updateRun(UUID id,RunInput input){
        Run old=run(id,true);version(old.version(),input.expectedVersion());
        jdbc.update("UPDATE checklist_runs SET title=?,notes=?,version=version+1 WHERE id=? AND agency_id=?",text(input.title()),text(input.notes()),id,agency());
        Run saved=run(id,false);event("RUN",id,saved.customerId(),"UPDATED",saved);return saved;
    }
    public Run cancelRun(UUID id,CancelInput input){
        Run old=run(id,true);version(old.version(),input.expectedVersion());
        if(old.cancelledAt()!=null)return old;
        jdbc.update("UPDATE checklist_runs SET cancelled_at=now(),cancel_reason=?,version=version+1 WHERE id=? AND agency_id=?",text(input.reason()),id,agency());
        // Lock the run before its tasks, consistently with task transitions. Batch writes avoid N+1.
        List<Task> open=jdbc.query(TASK_SELECT+"WHERE t.agency_id=? AND t.run_id=? AND t.state='OPEN' AND t.deleted_at IS NULL FOR UPDATE OF t",taskMapper,agency(),id);
        Set<UUID> changed=new HashSet<>();open.forEach(task->changed.add(task.id()));
        jdbc.update("UPDATE work_tasks SET state='CANCELLED',state_by=?,state_at=now(),state_reason=?,version=version+1,updated_at=now() WHERE agency_id=? AND run_id=? AND state='OPEN' AND deleted_at IS NULL",author(),text(input.reason()),agency(),id);
        taskEvents(tasks(null,null,null,State.CANCELLED,null,id).stream().filter(task->changed.contains(task.id())).toList(),"CANCELLED");
        Run saved=run(id,false);event("RUN",id,saved.customerId(),"CANCELLED",saved);return saved;
    }
    @Transactional(readOnly=true)
    public List<Event> history(String type,UUID id){
        if(!List.of("TASK","RUN","TEMPLATE").contains(type))bad("Tipo de histórico inválido.");
        // Audits remain available after logical deletion, but never across agencies.
        List<Event> events=jdbc.query("SELECT * FROM work_task_events WHERE agency_id=? AND resource_type=? AND resource_id=? ORDER BY created_at,id",
                (r,n)->new Event(r.getObject("id",UUID.class),r.getString("action"),read(r.getString("snapshot"),Object.class),r.getObject("author_id",UUID.class),r.getObject("created_at",OffsetDateTime.class)),agency(),type,id);
        if(events.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Histórico não encontrado nesta agência.");
        return events;
    }
}
