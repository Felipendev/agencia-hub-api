package com.agenciahub.api.application.usecases.financial.retrieve.byid;

import com.agenciahub.api.application.usecases.financial.shared.FinancialEntryResponseMapper;
import com.agenciahub.api.application.usecases.financial.shared.FinancialEntrySummaryResponseDTO;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.application.persistence.repository.FinancialEntryRepository;
import com.agenciahub.api.security.TenantContext;
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
        UUID agencyId = TenantContext.requireAgencyId();
        return financialEntryRepository.findByIdAndAgency_Id(id, agencyId)
                .map(financialEntryResponseMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("lançamento financeiro não encontrado: " + id));
    }
}
