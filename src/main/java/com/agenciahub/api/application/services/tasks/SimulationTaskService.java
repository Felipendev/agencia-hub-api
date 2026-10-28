package com.agenciahub.api.application.services.tasks;

import com.agenciahub.api.domain.AccumulationSimulation;
import com.agenciahub.api.domain.AccumulationSimulation.Assumptions;
import com.agenciahub.api.domain.AccumulationSimulation.Result;
import com.agenciahub.api.security.SecurityContextUsers;
import com.agenciahub.api.security.TenantContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class SimulationTaskService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final WorkService work;
    public record Step(@NotBlank @Size(max=180) String title,@Size(max=4000) String notes,@NotNull LocalDate dueOn) {}
    public record Input(@Min(1) int expectedVersion,@NotNull UUID customerId,@NotNull UUID responsibleId,
                        @NotEmpty @Size(max=20) List<@NotNull @Valid Step> steps) {}
    public record Source(UUID id,UUID customerId,String customerName,int version,Assumptions assumptions,Result result) {}
    public record Generation(UUID id,Source snapshot,Input requested,UUID authorId,String authorName,OffsetDateTime createdAt,List<WorkModels.Task> tasks) {}
    public record Context(LocalDate today,Source simulation,Generation generation) {}
    public record Summary(UUID sourceId,UUID customerId,String customerName,String title,int version,OffsetDateTime createdAt,boolean planDeleted) {}
    private UUID agency(){return TenantContext.requireAgencyId();}
    private static String text(String s){return s==null?"":s.trim();}
    private static void fail(HttpStatus status,String message){throw new ResponseStatusException(status,message);}
    private String json(Object object){
        try{return mapper.writeValueAsString(object);}catch(JsonProcessingException ex){throw new IllegalStateException("Não foi possível registrar a geração.",ex);}
    }
    private <T> T read(String json,Class<T> type){
        try{return mapper.readValue(json,type);}catch(JsonProcessingException ex){throw new IllegalStateException("Não foi possível consultar a geração.",ex);}
    }
    private Source source(UUID id,boolean lock){return source(id,lock,true);}
    private Source source(UUID id,boolean lock,boolean required){
        var rows=jdbc.query("SELECT s.*,c.name customer_name FROM accumulation_simulations s LEFT JOIN customers c ON c.id=s.customer_id AND c.agency_id=s.agency_id WHERE s.id=? AND s.agency_id=?"+(lock?" FOR UPDATE OF s":""),
            (r,n)->{var assumptions=read(r.getString("assumptions"),Assumptions.class);return new Source(r.getObject("id",UUID.class),r.getObject("customer_id",UUID.class),r.getString("customer_name"),r.getInt("version"),assumptions,AccumulationSimulation.calculate(assumptions));},id,agency());
        if(rows.isEmpty() && required)fail(HttpStatus.NOT_FOUND,"Plano não encontrado nesta agência.");
        return rows.isEmpty()?null:rows.get(0);
    }
    private List<Map<String,Object>> prior(UUID sourceId){
        return jdbc.queryForList("SELECT g.*,u.name author_name FROM simulation_task_generations g LEFT JOIN users u ON u.id=g.author_id AND u.agency_id=g.agency_id WHERE g.agency_id=? AND g.source_id=?",agency(),sourceId);
    }
    private Generation receipt(Map<String,Object> row){
        UUID id=(UUID)row.get("id");
        // JDBC map uses Timestamp for timestamptz; typed query keeps its UTC instant unambiguous.
        OffsetDateTime at=jdbc.queryForObject("SELECT created_at FROM simulation_task_generations WHERE id=? AND agency_id=?",(r,n)->r.getObject(1,OffsetDateTime.class),id,agency());
        return new Generation(id,read(row.get("snapshot").toString(),Source.class),read(row.get("request_payload").toString(),Input.class),(UUID)row.get("author_id"),(String)row.get("author_name"),at,work.generationTasks(id));
    }
    @Transactional(readOnly=true)
    public Context get(UUID id){
        var existing=prior(id);Source source=source(id,false,existing.isEmpty());
        return new Context(LocalDate.now(ZoneId.of("America/Fortaleza")),source,existing.isEmpty()?null:receipt(existing.get(0)));
    }
    @Transactional(readOnly=true)
    public List<Summary> list(){
        return jdbc.query("SELECT source_id,customer_id,snapshot->>'customerName' customer_name,source_title,source_version,created_at,simulation_id IS NULL plan_deleted FROM simulation_task_generations WHERE agency_id=? ORDER BY created_at DESC,id",
            (r,n)->new Summary(r.getObject("source_id",UUID.class),r.getObject("customer_id",UUID.class),r.getString("customer_name"),r.getString("source_title"),r.getInt("source_version"),r.getObject("created_at",OffsetDateTime.class),r.getBoolean("plan_deleted")),agency());
    }
    public Generation generate(UUID id,Input raw){
        Source source=source(id,true);
        Input input=new Input(raw.expectedVersion(),raw.customerId(),raw.responsibleId(),raw.steps().stream().map(s->new Step(text(s.title()),text(s.notes()),s.dueOn())).toList());
        var existing=prior(id);
        if(!existing.isEmpty()){
            if(!read(existing.get(0).get("request_payload").toString(),Object.class).equals(read(json(input),Object.class)))
                fail(HttpStatus.CONFLICT,"Este plano já gerou tarefas. Consulte a geração existente e edite as tarefas; uma nova confirmação não gera outro conjunto.");
            return receipt(existing.get(0));
        }
        if(source.customerId()==null)fail(HttpStatus.CONFLICT,"Associe uma pessoa a este plano legado antes de gerar tarefas.");
        if(source.version()!=input.expectedVersion() || !source.customerId().equals(input.customerId()))
            fail(HttpStatus.CONFLICT,"O plano ou a pessoa vinculada mudou. Recarregue para revisar a versão atual antes de gerar tarefas.");
        if(jdbc.queryForList("SELECT id FROM customers WHERE id=? AND agency_id=? FOR UPDATE",source.customerId(),agency()).isEmpty())fail(HttpStatus.NOT_FOUND,"Pessoa não encontrada nesta agência.");
        if(jdbc.queryForList("SELECT id FROM users WHERE id=? AND agency_id=? AND active=true AND account_kind IN ('AGENCY_OWNER','SALES_AGENT') FOR KEY SHARE",input.responsibleId(),agency()).isEmpty())
            fail(HttpStatus.BAD_REQUEST,"Selecione um responsável ativo desta agência.");
        UUID generation=UUID.randomUUID(),author=SecurityContextUsers.requireUserId();
        jdbc.update("INSERT INTO simulation_task_generations(id,agency_id,source_id,simulation_id,customer_id,source_version,source_title,snapshot,request_payload,author_id) VALUES(?,?,?,?,?,?,?,?::jsonb,?::jsonb,?)",
            generation,agency(),id,id,source.customerId(),source.version(),source.assumptions().title(),json(source),json(input),author);
        for(int i=0;i<input.steps().size();i++){
            Step step=input.steps().get(i);
            var task=work.create(UUID.randomUUID(),new WorkModels.TaskInput(WorkModels.Product.MILES,source.customerId(),step.title(),step.notes(),step.dueOn(),input.responsibleId(),null,null,0));
            jdbc.update("INSERT INTO simulation_task_links(agency_id,generation_id,task_id,position) VALUES(?,?,?,?)",agency(),generation,task.id(),i);
            jdbc.update("INSERT INTO work_task_events(id,agency_id,resource_type,resource_id,customer_id,action,snapshot,author_id) VALUES(?,?,'TASK',?,?,'PLAN_LINKED',?::jsonb,?)",
                UUID.randomUUID(),agency(),task.id(),source.customerId(),json(Map.of("title","Origem: "+source.assumptions().title(),"sourceVersion",source.version(),"simulationId",id,"assumptions",source.assumptions(),"customerId",source.customerId())),author);
        }
        return receipt(prior(id).get(0));
    }
}
