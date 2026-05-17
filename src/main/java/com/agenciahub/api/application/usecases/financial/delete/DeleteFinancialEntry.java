package com.agenciahub.api.application.usecases.financial.delete;

import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.application.persistence.repository.FinancialEntryRepository;
import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteFinancialEntry implements DeleteFinancialEntryUseCase {

    private final FinancialEntryRepository financialEntryRepository;

    @Override
    public void execute(UUID id) {
        UUID agencyId = TenantContext.requireAgencyId();
        var entry = financialEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("lançamento financeiro não encontrado: " + id));
        if (!entry.getAgency().getId().equals(agencyId)) {
            throw new ResourceNotFoundException("lançamento financeiro não encontrado: " + id);
        }
        financialEntryRepository.deleteById(id);
    }
}
