package com.agenciahub.api.application.usecases.financial.retrieve.list;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.financial.shared.FinancialEntrySummaryResponseDTO;

import java.util.List;

public interface ListFinancialEntriesUseCase extends UseCase<ListFinancialEntriesQuery, List<FinancialEntrySummaryResponseDTO>> {
}
