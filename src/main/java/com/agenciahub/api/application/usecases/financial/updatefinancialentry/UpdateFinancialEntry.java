package com.agenciahub.api.application.usecases.financial.updatefinancialentry;

import com.agenciahub.api.application.usecases.financial.FinancialEntryResponseMapper;
import com.agenciahub.api.dto.financial.FinancialEntryResponse;
import com.agenciahub.api.dto.financial.UpdateFinancialEntryRequest;
import com.agenciahub.api.entity.Customer;
import com.agenciahub.api.entity.FinancialEntry;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.CustomerRepository;
import com.agenciahub.api.repository.FinancialEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateFinancialEntry implements UpdateFinancialEntryUseCase {

    private final FinancialEntryRepository financialEntryRepository;
    private final CustomerRepository customerRepository;
    private final FinancialEntryResponseMapper financialEntryResponseMapper;

    @Override
    public FinancialEntryResponse execute(UpdateFinancialEntryCommand command) {
        UUID id = command.id();
        UpdateFinancialEntryRequest request = command.request();
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
        if (value == null) {
            return null;
        }
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
