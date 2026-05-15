package com.agenciahub.api.application.usecases.financial.create;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.financial.create.CreateFinancialEntryRequestDTO;
import com.agenciahub.api.application.usecases.financial.shared.FinancialEntrySummaryResponseDTO;

public interface CreateFinancialEntryUseCase extends UseCase<CreateFinancialEntryRequestDTO, FinancialEntrySummaryResponseDTO> {
}
