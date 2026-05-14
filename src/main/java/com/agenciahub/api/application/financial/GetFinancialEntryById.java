package com.agenciahub.api.application.financial;

import com.agenciahub.api.dto.financial.FinancialEntryResponse;
import com.agenciahub.api.service.FinancialEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetFinancialEntryById implements GetFinancialEntryByIdUseCase {

    private final FinancialEntryService financialEntryService;

    @Override
    public FinancialEntryResponse execute(UUID id) {
        return financialEntryService.getById(id);
    }
}
