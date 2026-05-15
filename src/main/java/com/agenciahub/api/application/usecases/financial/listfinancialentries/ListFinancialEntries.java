package com.agenciahub.api.application.usecases.financial.listfinancialentries;

import com.agenciahub.api.application.usecases.financial.FinancialEntryResponseMapper;
import com.agenciahub.api.dto.financial.FinancialEntryResponse;
import com.agenciahub.api.entity.FinancialEntry;
import com.agenciahub.api.repository.FinancialEntryRepository;
import com.agenciahub.api.repository.spec.FinancialEntrySpecifications;
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
    public List<FinancialEntryResponse> execute(ListFinancialEntriesQuery query) {
        Specification<FinancialEntry> spec = FinancialEntrySpecifications.withFilters(
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
