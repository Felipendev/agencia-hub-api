package com.agenciahub.api.application.usecases.quotation.getquotationbyid;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.dto.quotation.QuotationResponse;

import java.util.UUID;

public interface GetQuotationByIdUseCase extends UseCase<UUID, QuotationResponse> {
}
