package com.agenciahub.api.controller;

import com.agenciahub.api.dto.opportunity.CreateOpportunityRequest;
import com.agenciahub.api.dto.opportunity.OpportunityResponse;
import com.agenciahub.api.dto.opportunity.UpdateOpportunityRequest;
import com.agenciahub.api.service.OpportunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/opportunities")
@RequiredArgsConstructor
@Tag(name = "Opportunities")
public class OpportunityController {

    private final OpportunityService opportunityService;

    @GetMapping
    @Operation(summary = "List opportunities, optionally scoped to a customer")
    public List<OpportunityResponse> list(@RequestParam(required = false) UUID customerId) {
        return opportunityService.list(customerId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get opportunity by id")
    public OpportunityResponse get(@PathVariable UUID id) {
        return opportunityService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create opportunity")
    public OpportunityResponse create(@Valid @RequestBody CreateOpportunityRequest request) {
        return opportunityService.create(request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update opportunity")
    public OpportunityResponse patch(@PathVariable UUID id, @RequestBody UpdateOpportunityRequest request) {
        return opportunityService.update(id, request);
    }
}
