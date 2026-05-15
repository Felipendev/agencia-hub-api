package com.agenciahub.api.application.usecases.financial.getfinancialentrybyid;

import com.agenciahub.api.application.usecases.financial.FinancialEntryResponseMapper;
import com.agenciahub.api.dto.financial.FinancialEntryResponse;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.FinancialEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetFinancialEntryById implements GetFinancialEntryByIdUseCase {

    private final FinancialEntryRepository financialEntryRepository;
    private final FinancialEntryResponseMapper financialEntryResponseMapper;

    @Override
    public FinancialEntryResponse execute(UUID id) {
        return financialEntryRepository.findById(id)
                .map(financialEntryResponseMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("lançamento financeiro não encontrado: " + id));
    }
}
