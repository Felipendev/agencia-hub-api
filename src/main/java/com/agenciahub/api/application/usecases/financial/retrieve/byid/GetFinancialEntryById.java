package com.agenciahub.api.application.usecases.financial.retrieve.byid;

import com.agenciahub.api.application.usecases.financial.shared.FinancialEntryResponseMapper;
import com.agenciahub.api.application.usecases.financial.shared.FinancialEntrySummaryResponseDTO;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.application.persistence.repository.FinancialEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetFinancialEntryById implements GetFinancialEntryByIdUseCase {

    private final FinancialEntryRepository financialEntryRepository;
    private final FinancialEntryResponseMapper financialEntryResponseMapper;

    @Override
    public FinancialEntrySummaryResponseDTO execute(UUID id) {
        return financialEntryRepository.findById(id)
                .map(financialEntryResponseMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("lançamento financeiro não encontrado: " + id));
    }
}
