package com.agenciahub.api.application.usecases.quotation.getquotationbyid;

import com.agenciahub.api.application.usecases.quotation.QuotationResponseMapper;
import com.agenciahub.api.dto.quotation.QuotationResponse;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.QuotationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetQuotationById implements GetQuotationByIdUseCase {

    private final QuotationRepository quotationRepository;
    private final QuotationResponseMapper quotationResponseMapper;

    @Override
    public QuotationResponse execute(UUID id) {
        return quotationRepository
                .findById(id)
                .map(quotationResponseMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("cotação não encontrada: " + id));
    }
}
