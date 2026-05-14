package com.agenciahub.api.application.financial;

import com.agenciahub.api.dto.financial.FinancialEntryResponse;
import com.agenciahub.api.service.FinancialEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListFinancialEntries implements ListFinancialEntriesUseCase {

    private final FinancialEntryService financialEntryService;

    @Override
    public List<FinancialEntryResponse> execute(ListFinancialEntriesQuery query) {
        return financialEntryService.search(
                query.from(),
                query.to(),
                query.type(),
                query.category(),
                query.status(),
                query.customerId(),
                query.bankAccount());
    }
}
