package com.agenciahub.api.application.quotation;

import com.agenciahub.api.service.QuotationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteQuotation implements DeleteQuotationUseCase {

    private final QuotationService quotationService;

    @Override
    public void execute(UUID id) {
        quotationService.delete(id);
    }
}
