package com.agenciahub.api.application.usecases.quotation.retrieve.byid;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.quotation.shared.QuotationSummaryResponseDTO;

import java.util.UUID;

public interface GetQuotationByIdUseCase extends UseCase<UUID, QuotationSummaryResponseDTO> {
}
