package com.agenciahub.api.application.usecases.quotation.delete;

import com.agenciahub.api.application.persistence.entity.Quotation;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.application.persistence.repository.QuotationRepository;
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
