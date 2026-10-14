package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.services.contract.ContractTemplateService;
import com.agenciahub.api.domain.contract.Contracts.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Modelos de contrato: leitura pela equipe, manutenção pelo dono da agência. */
@RestController
@RequestMapping("/contract-templates")
@PreAuthorize("hasAnyRole('AGENCY_OWNER','SALES_AGENT')")
@RequiredArgsConstructor
public class ContractTemplateController {

    private final ContractTemplateService service;

    @GetMapping("/fields")
    public List<FieldView> fields() { return service.fields(); }

    @GetMapping
    public List<TemplateSummary> list(@RequestParam(defaultValue = "false") boolean includeArchived,
                                      @RequestParam(required = false) Product product) {
        return service.list(includeArchived, product);
    }

    @GetMapping("/{id}")
    public TemplateDetail get(@PathVariable UUID id) { return service.get(id); }

    @GetMapping("/{id}/versions/{version}")
    public TemplateVersion version(@PathVariable UUID id, @PathVariable int version) { return service.version(id, version); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('AGENCY_OWNER')")
    public TemplateDetail create(@Valid @RequestBody TemplateInput input) { return service.create(input); }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('AGENCY_OWNER')")
    public TemplateDetail update(@PathVariable UUID id, @Valid @RequestBody TemplateInput input) { return service.update(id, input); }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('AGENCY_OWNER')")
    public DeleteOutcome delete(@PathVariable UUID id, @RequestParam int expectedVersion) { return service.delete(id, expectedVersion); }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('AGENCY_OWNER')")
    public TemplateDetail restore(@PathVariable UUID id, @RequestParam int expectedVersion) { return service.restore(id, expectedVersion); }
}
