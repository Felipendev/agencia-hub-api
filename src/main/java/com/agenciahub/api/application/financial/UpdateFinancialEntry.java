package com.agenciahub.api.application.financial;

import com.agenciahub.api.dto.financial.FinancialEntryResponse;
import com.agenciahub.api.service.FinancialEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateFinancialEntry implements UpdateFinancialEntryUseCase {

    private final FinancialEntryService financialEntryService;

    @Override
    public FinancialEntryResponse execute(UpdateFinancialEntryCommand command) {
        return financialEntryService.update(command.id(), command.request());
    }
}
