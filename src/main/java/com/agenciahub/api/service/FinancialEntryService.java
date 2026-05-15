package com.agenciahub.api.service;

import com.agenciahub.api.domain.FinancialEntryCategory;
import com.agenciahub.api.domain.FinancialEntryStatus;
import com.agenciahub.api.domain.FinancialEntryType;
import com.agenciahub.api.dto.financial.CreateFinancialEntryRequest;
import com.agenciahub.api.dto.financial.FinancialEntryResponse;
import com.agenciahub.api.dto.financial.UpdateFinancialEntryRequest;
import com.agenciahub.api.application.financial.FinancialEntryResponseMapper;
import com.agenciahub.api.entity.Customer;
import com.agenciahub.api.entity.FinancialEntry;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.CustomerRepository;
import com.agenciahub.api.repository.FinancialEntryRepository;
import com.agenciahub.api.repository.spec.FinancialEntrySpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FinancialEntryService {

    private final FinancialEntryRepository financialEntryRepository;
    private final CustomerRepository customerRepository;
    private final FinancialEntryResponseMapper financialEntryResponseMapper;

    public List<FinancialEntryResponse> search(
            java.time.LocalDate from,
            java.time.LocalDate to,
            FinancialEntryType type,
            FinancialEntryCategory category,
            FinancialEntryStatus status,
            UUID customerId,
            String bankAccount) {

        Specification<FinancialEntry> spec = FinancialEntrySpecifications.withFilters(
                from, to, type, category, status, customerId, bankAccount);
        List<FinancialEntry> rows = financialEntryRepository.findAll(
                spec, Sort.by(Sort.Direction.DESC, "entryDate"));
        return rows.stream().map(financialEntryResponseMapper::toResponse).toList();
    }

    public FinancialEntryResponse getById(UUID id) {
        return financialEntryRepository.findById(id)
                .map(financialEntryResponseMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("lançamento financeiro não encontrado: " + id));
    }

    public FinancialEntryResponse create(CreateFinancialEntryRequest request) {
        Customer customer = resolveCustomer(request.customerId());
        FinancialEntry entity = FinancialEntry.builder()
                .description(request.description().strip())
                .type(request.type())
                .category(request.category())
                .amount(request.amount())
                .entryDate(request.entryDate())
                .status(request.status())
                .customer(customer)
                .bankAccount(blankToNull(request.bankAccount()))
                .build();
        FinancialEntry saved = financialEntryRepository.save(entity);
        return financialEntryResponseMapper.toResponse(saved);
    }

    public FinancialEntryResponse update(UUID id, UpdateFinancialEntryRequest request) {
        FinancialEntry entity = financialEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("lançamento financeiro não encontrado: " + id));
        if (request.description() != null) {
            entity.setDescription(request.description().strip());
        }
        if (request.type() != null) {
            entity.setType(request.type());
        }
        if (request.category() != null) {
            entity.setCategory(request.category());
        }
        if (request.amount() != null) {
            entity.setAmount(request.amount());
        }
        if (request.entryDate() != null) {
            entity.setEntryDate(request.entryDate());
        }
        if (request.status() != null) {
            entity.setStatus(request.status());
        }
        if (request.customerId() != null) {
            entity.setCustomer(resolveCustomer(request.customerId()));
        }
        if (request.bankAccount() != null) {
            entity.setBankAccount(blankToNull(request.bankAccount()));
        }
        return financialEntryResponseMapper.toResponse(financialEntryRepository.save(entity));
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String t = value.strip();
        return t.isEmpty() ? null : t;
    }

    private Customer resolveCustomer(UUID customerId) {
        if (customerId == null) {
            return null;
        }
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("cliente não encontrado: " + customerId));
    }
}
