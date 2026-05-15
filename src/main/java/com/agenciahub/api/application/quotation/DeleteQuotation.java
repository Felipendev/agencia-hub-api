package com.agenciahub.api.application.quotation;

import com.agenciahub.api.entity.Quotation;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.QuotationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteQuotation implements DeleteQuotationUseCase {

    private final QuotationRepository quotationRepository;

    @Override
    public void execute(UUID id) {
        Quotation entity = quotationRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("cotação não encontrada: " + id));
        quotationRepository.delete(entity);
    }
}
