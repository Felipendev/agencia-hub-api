package com.agenciahub.api.application.quotation;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.dto.quotation.QuotationResponse;

import java.util.List;

public interface ListQuotationsUseCase extends UseCase<ListQuotationsQuery, List<QuotationResponse>> {
}
