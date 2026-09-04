package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.entity.Supplier;
import com.agenciahub.api.application.persistence.repository.AgencyRepository;
import com.agenciahub.api.application.persistence.repository.SupplierRepository;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.security.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/suppliers")
@PreAuthorize("hasRole('AGENCY_OWNER')")
@RequiredArgsConstructor
public class SupplierController {
    private final SupplierRepository supplierRepository;
    private final AgencyRepository agencyRepository;

    @GetMapping
    public List<SupplierResponse> list() {
        return supplierRepository.findAllByAgency_IdOrderByNameAsc(TenantContext.requireAgencyId())
                .stream().map(SupplierResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierResponse create(@Valid @RequestBody CreateSupplierRequest request) {
        UUID agencyId = TenantContext.requireAgencyId();
        String name = request.name().strip();
        if (supplierRepository.existsByAgency_IdAndNameIgnoreCase(agencyId, name)) {
            throw new IllegalArgumentException("Já existe um fornecedor com este nome.");
        }
        Agency agency = agencyRepository.getReferenceById(agencyId);
        return SupplierResponse.from(supplierRepository.save(Supplier.builder().agency(agency).name(name)
                .contactName(blankToNull(request.contactName())).email(blankToNull(request.email()))
                .phone(blankToNull(request.phone())).notes(blankToEmpty(request.notes())).build()));
    }

    @PatchMapping("/{id}")
    public SupplierResponse patch(@PathVariable UUID id, @RequestBody UpdateSupplierRequest request) {
        Supplier supplier = findCurrentAgency(id);
        if (request.name() != null) supplier.setName(request.name().strip());
        if (request.contactName() != null) supplier.setContactName(blankToNull(request.contactName()));
        if (request.email() != null) supplier.setEmail(blankToNull(request.email()));
        if (request.phone() != null) supplier.setPhone(blankToNull(request.phone()));
        if (request.notes() != null) supplier.setNotes(request.notes().strip());
        return SupplierResponse.from(supplierRepository.save(supplier));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { supplierRepository.delete(findCurrentAgency(id)); }

    private Supplier findCurrentAgency(UUID id) {
        return supplierRepository.findByIdAndAgency_Id(id, TenantContext.requireAgencyId())
                .orElseThrow(() -> new ResourceNotFoundException("fornecedor não encontrado: " + id));
    }
    private static String blankToNull(String value) { return value == null || value.strip().isEmpty() ? null : value.strip(); }
    private static String blankToEmpty(String value) { return value == null ? "" : value.strip(); }

    public record CreateSupplierRequest(@NotBlank String name, String contactName, String email, String phone, String notes) {}
    public record UpdateSupplierRequest(String name, String contactName, String email, String phone, String notes) {}
    public record SupplierResponse(UUID id, String name, String contactName, String email, String phone, String notes, Instant createdAt) {
        static SupplierResponse from(Supplier supplier) { return new SupplierResponse(supplier.getId(), supplier.getName(), supplier.getContactName(), supplier.getEmail(), supplier.getPhone(), supplier.getNotes(), supplier.getCreatedAt()); }
    }
}
