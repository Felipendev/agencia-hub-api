package com.agenciahub.api.controller;

import com.agenciahub.api.dto.customer.CustomerResponse;
import com.agenciahub.api.dto.quotation.QuotationResponse;
import com.agenciahub.api.service.CustomerService;
import com.agenciahub.api.service.QuotationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/trash")
@RequiredArgsConstructor
@Tag(name = "Trash")
public class TrashController {

    private final QuotationService quotationService;
    private final CustomerService customerService;

    @GetMapping("/quotations")
    @Operation(summary = "List soft-deleted quotations")
    public List<QuotationResponse> listDeletedQuotations() {
        return quotationService.listDeleted();
    }

    @GetMapping("/customers")
    @Operation(summary = "List soft-deleted customers")
    public List<CustomerResponse> listDeletedCustomers() {
        return customerService.listDeleted();
    }

    @PostMapping("/quotations/{id}/restore")
    @Operation(summary = "Restore a soft-deleted quotation")
    public QuotationResponse restoreQuotation(@PathVariable UUID id) {
        return quotationService.restore(id);
    }

    @PostMapping("/customers/{id}/restore")
    @Operation(summary = "Restore a soft-deleted customer")
    public CustomerResponse restoreCustomer(@PathVariable UUID id) {
        return customerService.restore(id);
    }
}
