package com.agenciahub.api.application.usecases.financial.getfinancialentrybyid;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.dto.financial.FinancialEntryResponse;

import java.util.UUID;

public interface GetFinancialEntryByIdUseCase extends UseCase<UUID, FinancialEntryResponse> {
}
