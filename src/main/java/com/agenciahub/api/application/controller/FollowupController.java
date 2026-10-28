package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.services.followups.FollowupModels.*;
import com.agenciahub.api.application.services.followups.FollowupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/follow-ups")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('AGENCY_OWNER','SALES_AGENT')")
public class FollowupController {
    private final FollowupService service;
    @GetMapping public Board list(@RequestParam Product product,@RequestParam(required=false) UUID customerId,
            @RequestParam(required=false) UUID responsibleId,@RequestParam(required=false) State state,
            @RequestParam(defaultValue="ALL") Bucket bucket,@RequestParam(defaultValue="false") boolean deleted){return service.list(product,customerId,responsibleId,state,bucket,deleted);}
    @GetMapping("/{id}") public Followup get(@PathVariable UUID id){return service.get(id);}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Followup create(@RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody Input input){return service.create(key,input);}
    @PatchMapping("/{id}") public Followup update(@PathVariable UUID id,@Valid @RequestBody Input input){return service.update(id,input);}
    @PostMapping("/{id}/state") public Followup state(@PathVariable UUID id,@Valid @RequestBody Transition input){return service.transition(id,input);}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id,@RequestParam @Min(1) int expectedVersion){service.delete(id,expectedVersion);}
    @GetMapping("/{id}/history") public List<Event> history(@PathVariable UUID id){return service.history(id);}
}
