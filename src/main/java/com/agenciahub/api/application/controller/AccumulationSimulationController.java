package com.agenciahub.api.application.controller;

import com.agenciahub.api.domain.AccumulationSimulation;
import com.agenciahub.api.domain.AccumulationSimulation.Assumptions;
import com.agenciahub.api.domain.AccumulationSimulation.Result;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.exception.SimulationConflictException;
import com.agenciahub.api.security.SecurityContextUsers;
import com.agenciahub.api.security.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.*;

@RestController
@RequestMapping("/accumulation-simulations")
@PreAuthorize("hasAnyRole('AGENCY_OWNER','SALES_AGENT')")
@RequiredArgsConstructor
public class AccumulationSimulationController {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private UUID agency() { return TenantContext.requireAgencyId(); }
    private String author() { return SecurityContextUsers.requireUserId().toString(); }
    private static final String SELECT = "SELECT s.*,c.name customer_name FROM accumulation_simulations s LEFT JOIN customers c ON c.id=s.customer_id AND c.agency_id=s.agency_id ";

    @GetMapping
    public List<Saved> list(@RequestParam(required=false) UUID customerId,@RequestParam(defaultValue="false") boolean unlinked) {
        if(customerId!=null && unlinked) throw new IllegalArgumentException("Escolha uma pessoa ou os cenários sem cliente, separadamente.");
        if(customerId!=null) customer(customerId);
        String sql=SELECT+"WHERE s.agency_id=?";
        List<Object> args=new ArrayList<>();args.add(agency());
        if(customerId!=null){sql+=" AND s.customer_id=?";args.add(customerId);}
        if(unlinked)sql+=" AND s.customer_id IS NULL";
        return jdbc.query(sql+" ORDER BY s.updated_at DESC,s.id",this::map,args.toArray());
    }
    @GetMapping("/{id}")
    public Saved get(@PathVariable UUID id) {return find(id,false);}
    @GetMapping("/{id}/history")
    public List<Version> history(@PathVariable UUID id) {
        find(id,false);
        return jdbc.query("""
            SELECT v.*,c.name customer_name,u.name author_name
            FROM accumulation_simulation_versions v
            JOIN accumulation_simulations s ON s.id=v.simulation_id
            LEFT JOIN customers c ON c.id=v.customer_id AND c.agency_id=s.agency_id
            LEFT JOIN users u ON u.id::text=v.author AND u.agency_id=s.agency_id
            WHERE s.id=? AND s.agency_id=? ORDER BY v.version DESC
            """,(rs,n)->{
            Assumptions assumptions=read(rs.getString("assumptions"));
            return new Version(rs.getInt("version"),assumptions,AccumulationSimulation.calculate(assumptions),rs.getString("author"),rs.getString("author_name"),rs.getObject("recorded_at",OffsetDateTime.class),rs.getObject("customer_id",UUID.class),rs.getString("customer_name"));
        },id,agency());
    }
    @PostMapping @Transactional @ResponseStatus(HttpStatus.CREATED)
    public Saved create(@Valid @RequestBody Create request) {
        customer(request.customerId());
        UUID id=UUID.randomUUID();Assumptions assumptions=request.assumptions();AccumulationSimulation.calculate(assumptions);
        jdbc.update("INSERT INTO accumulation_simulations(id,agency_id,customer_id,title,assumptions,created_by,updated_by) VALUES(?,?,?,?,?::jsonb,?,?)",id,agency(),request.customerId(),assumptions.title().strip(),write(assumptions),author(),author());
        recordVersion(id,1,assumptions,request.customerId());return find(id,false);
    }
    @PatchMapping("/{id}") @Transactional
    public Saved update(@PathVariable UUID id,@Valid @RequestBody Update request) {
        Saved previous=find(id,true);
        if(previous.version()!=request.expectedVersion())throw new SimulationConflictException();
        // Missing customer on an older PATCH never clears a stored link.
        UUID customerId=request.customerId()==null?previous.customerId():request.customerId();
        if(customerId==null)throw new IllegalArgumentException("Selecione uma pessoa da agência para associar este cenário legado antes de salvar.");
        customer(customerId);
        if(previous.assumptions().equals(request.assumptions()) && customerId.equals(previous.customerId()))return previous;
        AccumulationSimulation.calculate(request.assumptions());int version=previous.version()+1;
        jdbc.update("UPDATE accumulation_simulations SET customer_id=?,title=?,assumptions=?::jsonb,version=?,updated_at=now(),updated_by=? WHERE id=? AND agency_id=?",customerId,request.assumptions().title().strip(),write(request.assumptions()),version,author(),id,agency());
        recordVersion(id,version,request.assumptions(),customerId);return find(id,false);
    }
    private void customer(UUID id) {
        if(id==null || !Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM customers WHERE id=? AND agency_id=?)",Boolean.class,id,agency())))throw new ResourceNotFoundException("pessoa não encontrada nesta agência");
    }
    private Saved find(UUID id,boolean lock) {
        return jdbc.query(SELECT+"WHERE s.id=? AND s.agency_id=?"+(lock?" FOR UPDATE OF s":""),this::map,id,agency()).stream().findFirst().orElseThrow(()->new ResourceNotFoundException("simulação não encontrada"));
    }
    private Saved map(java.sql.ResultSet rs,int n) throws java.sql.SQLException {
        Assumptions assumptions=read(rs.getString("assumptions"));
        return new Saved(rs.getObject("id",UUID.class),assumptions,AccumulationSimulation.calculate(assumptions),rs.getObject("created_at",OffsetDateTime.class),rs.getObject("updated_at",OffsetDateTime.class),rs.getInt("version"),rs.getObject("customer_id",UUID.class),rs.getString("customer_name"));
    }
    private void recordVersion(UUID id,int version,Assumptions assumptions,UUID customerId) {
        jdbc.update("INSERT INTO accumulation_simulation_versions(id,simulation_id,version,assumptions,author,customer_id) VALUES(?,?,?,?::jsonb,?,?)",UUID.randomUUID(),id,version,write(assumptions),author(),customerId);
    }
    @DeleteMapping("/{id}") @Transactional @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id,@RequestParam(required=false) Integer expectedVersion) {
        Saved previous=find(id,true);
        if(expectedVersion!=null && previous.version()!=expectedVersion)throw new SimulationConflictException();
        jdbc.update("DELETE FROM accumulation_simulations WHERE id=? AND agency_id=?",id,agency());
    }
    private Assumptions read(String json) {
        try{return mapper.readValue(json,Assumptions.class);}catch(java.io.IOException e){throw new IllegalStateException("Não foi possível ler a simulação salva.",e);}
    }
    private String write(Assumptions input) {
        try{return mapper.writeValueAsString(input);}catch(java.io.IOException e){throw new IllegalStateException("Não foi possível salvar a simulação.",e);}
    }
    public record Create(@NotNull UUID customerId,@NotNull @Valid Assumptions assumptions) {}
    public record Update(@NotNull @Valid Assumptions assumptions,@NotNull @Min(1) Integer expectedVersion,UUID customerId) {}
    public record Saved(UUID id,Assumptions assumptions,Result result,OffsetDateTime createdAt,OffsetDateTime updatedAt,int version,UUID customerId,String customerName) {}
    public record Version(int version,Assumptions assumptions,Result result,String author,String authorName,OffsetDateTime recordedAt,UUID customerId,String customerName) {}
}
