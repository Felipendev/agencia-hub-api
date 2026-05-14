package com.agenciahub.api.application.financial;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.dto.financial.CreateFinancialEntryRequest;
import com.agenciahub.api.dto.financial.FinancialEntryResponse;

public interface CreateFinancialEntryUseCase extends UseCase<CreateFinancialEntryRequest, FinancialEntryResponse> {
}
