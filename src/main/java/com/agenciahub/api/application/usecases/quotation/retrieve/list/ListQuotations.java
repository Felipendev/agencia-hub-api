package com.agenciahub.api.application.usecases.quotation.retrieve.list;

import com.agenciahub.api.application.usecases.quotation.shared.QuotationSupport;
import com.agenciahub.api.application.usecases.quotation.shared.QuotationResponseMapper;
import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.application.usecases.quotation.shared.QuotationSummaryResponseDTO;
import com.agenciahub.api.application.persistence.entity.Quotation;
import com.agenciahub.api.application.persistence.repository.QuotationRepository;
import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListQuotations implements ListQuotationsUseCase {

    private final QuotationRepository quotationRepository;
    private final QuotationResponseMapper quotationResponseMapper;

    @Override
    @Transactional(readOnly = true)
    public List<QuotationSummaryResponseDTO> execute(ListQuotationsQuery query) {
        UUID agencyId = TenantContext.requireAgencyId();
        UUID callerId = query.caller() != null ? query.caller().getId() : null;
        AccountKind callerRole = query.caller() != null ? query.caller().getAccountKind() : AccountKind.AGENCY_OWNER;
        UUID effectiveSellerId = (callerRole == AccountKind.SALES_AGENT) ? callerId : null;

        var spec = QuotationSupport.quotationSearchSpec(
                agencyId, query.customerId(), query.status(), query.search(), effectiveSellerId);
        List<Quotation> rows = quotationRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
        return rows.stream().map(quotationResponseMapper::toResponse).toList();
    }
}
