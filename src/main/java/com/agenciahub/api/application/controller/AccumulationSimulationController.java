package com.agenciahub.api.application.controller;

import com.agenciahub.api.domain.AccumulationSimulation;
import com.agenciahub.api.domain.AccumulationSimulation.Assumptions;
import com.agenciahub.api.domain.AccumulationSimulation.Result;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.security.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/accumulation-simulations")
@PreAuthorize("hasAnyRole('AGENCY_OWNER','SALES_AGENT')")
@RequiredArgsConstructor
public class AccumulationSimulationController {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    @GetMapping
    public List<Saved> list() {
        return jdbc.query("SELECT id,assumptions,created_at,updated_at,version FROM accumulation_simulations WHERE agency_id=? ORDER BY updated_at DESC,id",this::map,TenantContext.requireAgencyId());
    }
    @GetMapping("/{id}")
    public Saved get(@PathVariable UUID id) {return find(id,false);}
    @GetMapping("/{id}/history")
    public List<Version> history(@PathVariable UUID id) {
        find(id,false);
        return jdbc.query("SELECT v.version,v.assumptions,v.author,v.recorded_at FROM accumulation_simulation_versions v JOIN accumulation_simulations s ON s.id=v.simulation_id WHERE s.id=? AND s.agency_id=? ORDER BY v.version DESC",(rs,n)->{
            Assumptions assumptions=read(rs.getString("assumptions"));
            return new Version(rs.getInt("version"),assumptions,AccumulationSimulation.calculate(assumptions),rs.getString("author"),rs.getObject("recorded_at",OffsetDateTime.class));
        },id,TenantContext.requireAgencyId());
    }
    @PostMapping @Transactional @ResponseStatus(HttpStatus.CREATED)
    public Saved create(@Valid @RequestBody Assumptions assumptions, Principal principal) {
        UUID id=UUID.randomUUID(); AccumulationSimulation.calculate(assumptions);
        jdbc.update("INSERT INTO accumulation_simulations(id,agency_id,title,assumptions,created_by,updated_by) VALUES(?,?,?,?::jsonb,?,?)",id,TenantContext.requireAgencyId(),assumptions.title().strip(),write(assumptions),principal.getName(),principal.getName());
        recordVersion(id,1,assumptions,principal.getName());
        return find(id,false);
    }
    @PatchMapping("/{id}") @Transactional
    public Saved update(@PathVariable UUID id,@Valid @RequestBody Update request,Principal principal) {
        Saved previous=find(id,true);
        if(previous.assumptions().equals(request.assumptions()))return previous;
        if(previous.version()!=request.expectedVersion())throw new com.agenciahub.api.exception.SimulationConflictException();
        AccumulationSimulation.calculate(request.assumptions());
        int version=previous.version()+1;
        jdbc.update("UPDATE accumulation_simulations SET title=?,assumptions=?::jsonb,version=?,updated_at=now(),updated_by=? WHERE id=? AND agency_id=?",request.assumptions().title().strip(),write(request.assumptions()),version,principal.getName(),id,TenantContext.requireAgencyId());
        recordVersion(id,version,request.assumptions(),principal.getName());
        return find(id,false);
    }
    private Saved find(UUID id,boolean lock) {
        return jdbc.query("SELECT id,assumptions,created_at,updated_at,version FROM accumulation_simulations WHERE id=? AND agency_id=?"+(lock?" FOR UPDATE":""),this::map,id,TenantContext.requireAgencyId())
            .stream().findFirst().orElseThrow(()->new ResourceNotFoundException("simulação não encontrada"));
    }
    private Saved map(java.sql.ResultSet rs,int n) throws java.sql.SQLException {
        Assumptions assumptions=read(rs.getString("assumptions"));
        return new Saved(rs.getObject("id",UUID.class),assumptions,AccumulationSimulation.calculate(assumptions),rs.getObject("created_at",OffsetDateTime.class),rs.getObject("updated_at",OffsetDateTime.class),rs.getInt("version"));
    }
    private void recordVersion(UUID id,int version,Assumptions assumptions,String author) {
        jdbc.update("INSERT INTO accumulation_simulation_versions(id,simulation_id,version,assumptions,author) VALUES(?,?,?,?::jsonb,?)",UUID.randomUUID(),id,version,write(assumptions),author);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        if(jdbc.update("DELETE FROM accumulation_simulations WHERE id=? AND agency_id=?",id,TenantContext.requireAgencyId())==0) throw new ResourceNotFoundException("simulação não encontrada");
    }
    private Assumptions read(String json) {
        try{return mapper.readValue(json,Assumptions.class);}catch(java.io.IOException e){throw new IllegalStateException("Não foi possível ler a simulação salva.",e);}
    }
    private String write(Assumptions input) {
        try{return mapper.writeValueAsString(input);}catch(java.io.IOException e){throw new IllegalStateException("Não foi possível salvar a simulação.",e);}
    }
    public record Update(@jakarta.validation.constraints.NotNull @Valid Assumptions assumptions,@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(1) Integer expectedVersion) {}
    public record Saved(UUID id,Assumptions assumptions,Result result,OffsetDateTime createdAt,OffsetDateTime updatedAt,int version) {}
    public record Version(int version,Assumptions assumptions,Result result,String author,OffsetDateTime recordedAt) {}
}
