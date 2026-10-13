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
        return jdbc.query("SELECT id,assumptions,created_at FROM accumulation_simulations WHERE agency_id=? ORDER BY created_at DESC,id", (rs,n)-> {
            Assumptions assumptions=read(rs.getString("assumptions"));
            return new Saved(rs.getObject("id",UUID.class),assumptions,AccumulationSimulation.calculate(assumptions),rs.getObject("created_at",OffsetDateTime.class));
        },TenantContext.requireAgencyId());
    }
    @PostMapping @Transactional @ResponseStatus(HttpStatus.CREATED)
    public Saved create(@Valid @RequestBody Assumptions assumptions, Principal principal) {
        UUID id=UUID.randomUUID(); Result result=AccumulationSimulation.calculate(assumptions);
        OffsetDateTime created=jdbc.queryForObject("INSERT INTO accumulation_simulations(id,agency_id,title,assumptions,created_by) VALUES(?,?,?,?::jsonb,?) RETURNING created_at",OffsetDateTime.class,id,TenantContext.requireAgencyId(),assumptions.title().strip(),write(assumptions),principal.getName());
        return new Saved(id,assumptions,result,created);
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
    public record Saved(UUID id,Assumptions assumptions,Result result,OffsetDateTime createdAt) {}
}
