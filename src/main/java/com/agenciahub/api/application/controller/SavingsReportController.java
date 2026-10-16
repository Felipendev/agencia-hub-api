package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.services.savings.SavingsReportService;
import com.agenciahub.api.application.services.savings.SavingsReportService.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Relatório compartilhável de economia: ação do gestor da agência. */
@RestController
@RequestMapping("/savings/shared-reports")
@PreAuthorize("hasRole('AGENCY_OWNER')")
@RequiredArgsConstructor
public class SavingsReportController {

    private final SavingsReportService service;

    @PostMapping("/preview")
    public Content preview(@Valid @RequestBody ReportInput input) { return service.preview(input); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SharedReport create(@Valid @RequestBody ReportInput input) { return service.create(input); }

    @GetMapping
    public List<SharedReportSummary> list(@RequestParam(required = false) UUID customerId) { return service.list(customerId); }

    @GetMapping("/{id}")
    public SharedReport get(@PathVariable UUID id) { return service.get(id); }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id) { return TripSavingsController.file(service.pdf(id)); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }
}
