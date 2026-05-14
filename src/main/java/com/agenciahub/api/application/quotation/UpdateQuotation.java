package com.agenciahub.api.application.quotation;

import com.agenciahub.api.dto.quotation.QuotationResponse;
import com.agenciahub.api.service.QuotationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateQuotation implements UpdateQuotationUseCase {

    private final QuotationService quotationService;

    @Override
    public QuotationResponse execute(UpdateQuotationCommand command) {
        return quotationService.update(command.id(), command.request());
    }
}
