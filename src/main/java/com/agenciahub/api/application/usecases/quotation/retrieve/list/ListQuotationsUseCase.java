package com.agenciahub.api.application.usecases.quotation.retrieve.list;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.quotation.shared.QuotationSummaryResponseDTO;

import java.util.List;

public interface ListQuotationsUseCase extends UseCase<ListQuotationsQuery, List<QuotationSummaryResponseDTO>> {
}
