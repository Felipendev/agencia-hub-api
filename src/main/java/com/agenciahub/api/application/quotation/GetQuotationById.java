package com.agenciahub.api.application.quotation;

import com.agenciahub.api.dto.quotation.QuotationResponse;
import com.agenciahub.api.service.QuotationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetQuotationById implements GetQuotationByIdUseCase {

    private final QuotationService quotationService;

    @Override
    public QuotationResponse execute(UUID id) {
        return quotationService.getById(id);
    }
}
