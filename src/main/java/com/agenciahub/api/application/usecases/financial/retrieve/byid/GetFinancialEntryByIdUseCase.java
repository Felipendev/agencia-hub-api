package com.agenciahub.api.application.usecases.financial.retrieve.byid;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.financial.shared.FinancialEntrySummaryResponseDTO;

import java.util.UUID;

public interface GetFinancialEntryByIdUseCase extends UseCase<UUID, FinancialEntrySummaryResponseDTO> {
}
