package com.agenciahub.api.application.quotation;

import com.agenciahub.api.domain.UserRole;
import com.agenciahub.api.dto.quotation.QuotationResponse;
import com.agenciahub.api.entity.Quotation;
import com.agenciahub.api.repository.QuotationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListQuotations implements ListQuotationsUseCase {

    private final QuotationRepository quotationRepository;
    private final QuotationResponseMapper quotationResponseMapper;

    @Override
    public List<QuotationResponse> execute(ListQuotationsQuery query) {
        UUID callerId = query.caller() != null ? query.caller().getId() : null;
        UserRole callerRole = query.caller() != null ? query.caller().getRole() : UserRole.OWNER;
        UUID effectiveSellerId = (callerRole == UserRole.SELLER) ? callerId : null;

        var spec = QuotationSupport.quotationSearchSpec(
                query.customerId(), query.status(), query.search(), effectiveSellerId);
        List<Quotation> rows = quotationRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
        return rows.stream().map(quotationResponseMapper::toResponse).toList();
    }
}
