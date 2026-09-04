package com.agenciahub.api.application.usecases.quotation.retrieve.byid;

import com.agenciahub.api.application.usecases.quotation.shared.QuotationResponseMapper;
import com.agenciahub.api.application.usecases.quotation.shared.QuotationSummaryResponseDTO;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.application.persistence.repository.QuotationRepository;
import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetQuotationById implements GetQuotationByIdUseCase {

    private final QuotationRepository quotationRepository;
    private final QuotationResponseMapper quotationResponseMapper;

    @Override
    public QuotationSummaryResponseDTO execute(UUID id) {
        UUID agencyId = TenantContext.requireAgencyId();
        return quotationRepository
                .findByIdAndAgency_Id(id, agencyId)
                .map(quotationResponseMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("cotação não encontrada: " + id));
    }
}
