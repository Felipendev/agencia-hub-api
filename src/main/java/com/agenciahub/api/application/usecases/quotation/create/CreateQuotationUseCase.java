package com.agenciahub.api.application.usecases.quotation.create;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.quotation.shared.QuotationSummaryResponseDTO;

public interface CreateQuotationUseCase extends UseCase<CreateQuotationCommand, QuotationSummaryResponseDTO> {
}
