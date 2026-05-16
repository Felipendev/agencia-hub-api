package com.agenciahub.api.application.usecases.financial.update;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.financial.shared.FinancialEntrySummaryResponseDTO;

public interface UpdateFinancialEntryUseCase extends UseCase<UpdateFinancialEntryCommand, FinancialEntrySummaryResponseDTO> {
}
