package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.services.tasks.WorkService;
import com.agenciahub.api.application.services.tasks.WorkModels.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/work")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('AGENCY_OWNER','SALES_AGENT')")
public class WorkController {
    private final WorkService service;
    @GetMapping("/members") public List<Member> members(){return service.members();}
    @GetMapping("/references") public References references(@RequestParam UUID customerId){return service.references(customerId);}
    @GetMapping("/tasks") public List<Task> tasks(@RequestParam(required=false) Product product,
            @RequestParam(required=false) UUID customerId,@RequestParam(required=false) UUID responsibleId,
            @RequestParam(required=false) State state,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate dueBefore,
            @RequestParam(required=false) UUID runId,@RequestParam(required=false) UUID simulationId){return service.tasks(product,customerId,responsibleId,state,dueBefore,runId,simulationId);}
    @GetMapping("/tasks/{id}") public Task task(@PathVariable UUID id){return service.task(id);}
    @PostMapping("/tasks") @ResponseStatus(HttpStatus.CREATED)
    public Task create(@RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody TaskInput input){return service.create(key,input);}
    @PatchMapping("/tasks/{id}") public Task update(@PathVariable UUID id,@Valid @RequestBody TaskInput input){return service.update(id,input);}
    @DeleteMapping("/tasks/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable UUID id,@RequestParam @Min(1) int expectedVersion){service.deleteTask(id,expectedVersion);}
    @PostMapping("/tasks/{id}/state") public Task state(@PathVariable UUID id,@Valid @RequestBody Transition input){return service.transition(id,input);}
    @GetMapping("/templates") public List<Template> templates(@RequestParam(required=false) Product product){return service.templates(product);}
    @PostMapping("/templates") @ResponseStatus(HttpStatus.CREATED)
    public Template createTemplate(@Valid @RequestBody TemplateInput input){return service.saveTemplate(null,input);}
    @PatchMapping("/templates/{id}") public Template updateTemplate(@PathVariable UUID id,@Valid @RequestBody TemplateInput input){return service.saveTemplate(id,input);}
    @DeleteMapping("/templates/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTemplate(@PathVariable UUID id,@RequestParam @Min(1) int expectedVersion){service.deleteTemplate(id,expectedVersion);}
    @PostMapping("/templates/{id}/apply") public Run apply(@PathVariable UUID id,@Valid @RequestBody ApplyInput input){return service.apply(id,input);}
    @GetMapping("/runs") public List<Run> runs(@RequestParam(required=false) Product product,@RequestParam(required=false) UUID customerId){return service.runs(product,customerId);}
    @GetMapping("/runs/{id}") public Run run(@PathVariable UUID id){return service.run(id,false);}
    @PatchMapping("/runs/{id}") public Run updateRun(@PathVariable UUID id,@Valid @RequestBody RunInput input){return service.updateRun(id,input);}
    @PostMapping("/runs/{id}/cancel") public Run cancel(@PathVariable UUID id,@Valid @RequestBody CancelInput input){return service.cancelRun(id,input);}
    @GetMapping("/history/{type}/{id}") public List<Event> history(@PathVariable String type,@PathVariable UUID id){return service.history(type,id);}
}
