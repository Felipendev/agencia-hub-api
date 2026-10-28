package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.services.diagnostics.DiagnosticService;
import com.agenciahub.api.application.services.diagnostics.DiagnosticDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController @RequiredArgsConstructor @RequestMapping("/miles-diagnostics")
@PreAuthorize("hasAnyRole('AGENCY_OWNER','SALES_AGENT')")
public class MilesDiagnosticController {
    private final DiagnosticService service;
    @GetMapping("/config") public Config config(){return service.config();}
    @PutMapping("/config") @PreAuthorize("hasRole('AGENCY_OWNER')") public Config configure(@Valid @RequestBody ConfigInput in){return service.configure(in);}
    @DeleteMapping("/config") @PreAuthorize("hasRole('AGENCY_OWNER')") public Config revoke(@RequestParam int expectedVersion){return service.revoke(expectedVersion);}
    @GetMapping public List<Summary> list(){return service.list();}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public Entry create(@RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody Data in){return service.createStaff(key,in);}
    @GetMapping("/{id}") public Entry get(@PathVariable UUID id){return service.get(id);}
    @PatchMapping("/{id}") public Entry edit(@PathVariable UUID id,@Valid @RequestBody Edit in){return service.edit(id,in);}
    @DeleteMapping("/{id}") public Entry archive(@PathVariable UUID id,@Valid @RequestBody State in){return service.archive(id,in,true);}
    @PostMapping("/{id}/restore") public Entry restore(@PathVariable UUID id,@Valid @RequestBody State in){return service.archive(id,in,false);}
    @GetMapping("/{id}/events") public List<Event> events(@PathVariable UUID id){return service.events(id);}
    @PostMapping("/{id}/incorporate") public Receipt incorporate(@PathVariable UUID id,@Valid @RequestBody Apply in){return service.incorporate(id,in);}
}
