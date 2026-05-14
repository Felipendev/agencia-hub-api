package com.agenciahub.api.application.quotation;

import com.agenciahub.api.domain.UserRole;
import com.agenciahub.api.dto.quotation.QuotationResponse;
import com.agenciahub.api.service.QuotationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListQuotations implements ListQuotationsUseCase {

    private final QuotationService quotationService;

    @Override
    public List<QuotationResponse> execute(ListQuotationsQuery query) {
        UUID callerId = query.caller() != null ? query.caller().getId() : null;
        UserRole callerRole = query.caller() != null ? query.caller().getRole() : UserRole.OWNER;
        return quotationService.search(
                query.customerId(), query.status(), query.search(), callerId, callerRole);
    }
}
