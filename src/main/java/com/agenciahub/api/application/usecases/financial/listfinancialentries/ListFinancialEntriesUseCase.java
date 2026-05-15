package com.agenciahub.api.application.usecases.financial.listfinancialentries;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.dto.financial.FinancialEntryResponse;

import java.util.List;

public interface ListFinancialEntriesUseCase extends UseCase<ListFinancialEntriesQuery, List<FinancialEntryResponse>> {
}
