package com.agenciahub.api.application.controller;
import com.agenciahub.api.application.services.tasks.SimulationTaskService;
import com.agenciahub.api.application.services.tasks.SimulationTaskService.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/accumulation-simulations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('AGENCY_OWNER','SALES_AGENT')")
public class SimulationTaskController {
    private final SimulationTaskService service;
    @GetMapping("/task-generations") public java.util.List<Summary> list(){return service.list();}
    @GetMapping("/{id}/task-generation") public Context get(@PathVariable UUID id){return service.get(id);}
    @PostMapping("/{id}/task-generation") public Generation generate(@PathVariable UUID id,@Valid @RequestBody Input input){return service.generate(id,input);}
}
