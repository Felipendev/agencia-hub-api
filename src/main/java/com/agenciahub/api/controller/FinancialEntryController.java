package com.agenciahub.api.controller;

import com.agenciahub.api.domain.FinancialEntryCategory;
import com.agenciahub.api.domain.FinancialEntryStatus;
import com.agenciahub.api.domain.FinancialEntryType;
import com.agenciahub.api.dto.financial.CreateFinancialEntryRequest;
import com.agenciahub.api.dto.financial.FinancialEntryResponse;
import com.agenciahub.api.dto.financial.UpdateFinancialEntryRequest;
import com.agenciahub.api.service.FinancialEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/financial-entries")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
@Tag(name = "Financial entries")
public class FinancialEntryController {

    private final FinancialEntryService financialEntryService;

    @GetMapping
    @Operation(summary = "List financial entries with optional filters")
    public List<FinancialEntryResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) FinancialEntryType type,
            @RequestParam(required = false) FinancialEntryCategory category,
            @RequestParam(required = false) FinancialEntryStatus status,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) String bankAccount) {
        return financialEntryService.search(from, to, type, category, status, customerId, bankAccount);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get financial entry by id")
    public FinancialEntryResponse get(@PathVariable UUID id) {
        return financialEntryService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create financial entry")
    public FinancialEntryResponse create(@Valid @RequestBody CreateFinancialEntryRequest request) {
        return financialEntryService.create(request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update financial entry")
    public FinancialEntryResponse patch(@PathVariable UUID id, @RequestBody UpdateFinancialEntryRequest request) {
        return financialEntryService.update(id, request);
    }
}
