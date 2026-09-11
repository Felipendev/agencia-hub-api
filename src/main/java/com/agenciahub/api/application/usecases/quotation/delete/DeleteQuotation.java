package com.agenciahub.api.application.usecases.quotation.delete;

import com.agenciahub.api.application.persistence.entity.Quotation;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.application.persistence.repository.QuotationRepository;
import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteQuotation implements DeleteQuotationUseCase {

    private final QuotationRepository quotationRepository;
    private final com.agenciahub.api.application.persistence.repository.SaleRepository sales;

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void execute(UUID id) {
        UUID agencyId = TenantContext.requireAgencyId();
        Quotation entity = quotationRepository
                .findForUpdate(id, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("cotação não encontrada: " + id));
        if (!sales.findByQuotation_IdOrderByCreatedAtAsc(id).isEmpty())
            throw new IllegalArgumentException("Esta cotação possui venda vinculada. Cancele a cotação para preservar o histórico.");
        quotationRepository.delete(entity);
    }
}
