package com.agenciahub.api.application.usecases.financial.retrieve.list;

import com.agenciahub.api.application.usecases.financial.shared.FinancialEntryResponseMapper;
import com.agenciahub.api.application.usecases.financial.shared.FinancialEntrySummaryResponseDTO;
import com.agenciahub.api.application.persistence.entity.FinancialEntry;
import com.agenciahub.api.application.persistence.repository.FinancialEntryRepository;
import com.agenciahub.api.application.persistence.repository.spec.FinancialEntrySpecifications;
import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListFinancialEntries implements ListFinancialEntriesUseCase {

    private final FinancialEntryRepository financialEntryRepository;
    private final FinancialEntryResponseMapper financialEntryResponseMapper;

    @Override
    public List<FinancialEntrySummaryResponseDTO> execute(ListFinancialEntriesQuery query) {
        java.util.UUID agencyId = TenantContext.requireAgencyId();
        Specification<FinancialEntry> spec = FinancialEntrySpecifications.withFilters(
                agencyId,
                query.from(),
                query.to(),
                query.type(),
                query.category(),
                query.status(),
                query.customerId(),
                query.bankAccount());
        List<FinancialEntry> rows = financialEntryRepository.findAll(
                spec, Sort.by(Sort.Direction.DESC, "entryDate"));
        return rows.stream().map(financialEntryResponseMapper::toResponse).toList();
    }
}
