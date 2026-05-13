package com.agenciahub.api.controller;

import com.agenciahub.api.domain.CustomerStatus;
import com.agenciahub.api.dto.customer.CreateCustomerRequest;
import com.agenciahub.api.dto.customer.CustomerResponse;
import com.agenciahub.api.dto.customer.UpdateCustomerRequest;
import com.agenciahub.api.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/customers")
@RequiredArgsConstructor
@Tag(name = "Customers")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @Operation(summary = "List customers with optional filters")
    public List<CustomerResponse> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) CustomerStatus status) {
        return customerService.search(name, status);
    }

    @GetMapping("/lookup")
    @Operation(summary = "Find active customer by e-mail or phone (import flow)")
    public ResponseEntity<CustomerResponse> lookup(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone) {
        return customerService.lookupActiveByContact(email, phone)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by id")
    public CustomerResponse get(@PathVariable UUID id) {
        return customerService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create customer")
    public CustomerResponse create(@Valid @RequestBody CreateCustomerRequest request) {
        return customerService.create(request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update customer")
    public CustomerResponse patch(@PathVariable UUID id, @RequestBody UpdateCustomerRequest request) {
        return customerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Permanently delete customer (and related quotations/opportunities)")
    public void delete(@PathVariable UUID id) {
        customerService.delete(id);
    }
}
