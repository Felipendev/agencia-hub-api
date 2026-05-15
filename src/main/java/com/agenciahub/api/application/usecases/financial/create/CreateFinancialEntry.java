package com.agenciahub.api.application.usecases.financial.create;

import com.agenciahub.api.application.usecases.financial.shared.FinancialEntryResponseMapper;
import com.agenciahub.api.application.usecases.financial.create.CreateFinancialEntryRequestDTO;
import com.agenciahub.api.application.usecases.financial.shared.FinancialEntrySummaryResponseDTO;
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
public class CreateFinancialEntry implements CreateFinancialEntryUseCase {

    private final FinancialEntryRepository financialEntryRepository;
    private final CustomerRepository customerRepository;
    private final FinancialEntryResponseMapper financialEntryResponseMapper;

    @Override
    public FinancialEntrySummaryResponseDTO execute(CreateFinancialEntryRequestDTO request) {
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
