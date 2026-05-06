package com.agenciahub.api.controller;

import com.agenciahub.api.domain.QuotationStatus;
import com.agenciahub.api.domain.UserRole;
import com.agenciahub.api.dto.quotation.CreateQuotationRequest;
import com.agenciahub.api.dto.quotation.QuotationResponse;
import com.agenciahub.api.dto.quotation.UpdateQuotationRequest;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.service.QuotationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/quotations")
@RequiredArgsConstructor
@Tag(name = "Quotations")
public class QuotationController {

    private final QuotationService quotationService;

    @GetMapping
    @Operation(summary = "List quotations. SELLERs see only their own; OWNERs see all.")
    public List<QuotationResponse> list(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) QuotationStatus status,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal User caller) {

        UUID callerId   = caller != null ? caller.getId()   : null;
        UserRole callerRole = caller != null ? caller.getRole() : UserRole.OWNER;

        return quotationService.search(customerId, status, search, callerId, callerRole);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get quotation by id")
    public QuotationResponse get(@PathVariable UUID id) {
        return quotationService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create quotation. If caller is SELLER and no sellerId provided, auto-assigns to caller.")
    public QuotationResponse create(
            @Valid @RequestBody CreateQuotationRequest request,
            @AuthenticationPrincipal User caller) {

        // Auto-assign seller when caller is a SELLER and no sellerId was provided
        if (caller != null && caller.getRole() == UserRole.SELLER && request.sellerId() == null) {
            request = new CreateQuotationRequest(
                    request.customerId(), request.opportunityId(), caller.getId(),
                    request.title(), request.destination(), request.description(),
                    request.totalAmount(), request.currency(), request.status(),
                    request.validUntil(), request.travelStartDate(), request.travelEndDate(),
                    request.details(), request.tags(), request.priority(),
                    request.assignee(), request.internalNotes()
            );
        }
        return quotationService.create(request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update quotation")
    public QuotationResponse patch(@PathVariable UUID id,
                                   @RequestBody UpdateQuotationRequest request) {
        return quotationService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete a quotation")
    public void delete(@PathVariable UUID id) {
        quotationService.softDelete(id);
    }
}
