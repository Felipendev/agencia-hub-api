package com.agenciahub.api.application.quotation;

import com.agenciahub.api.domain.UserRole;
import com.agenciahub.api.dto.quotation.CreateQuotationRequest;
import com.agenciahub.api.dto.quotation.QuotationResponse;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.service.QuotationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateQuotation implements CreateQuotationUseCase {

    private final QuotationService quotationService;

    @Override
    public QuotationResponse execute(CreateQuotationCommand command) {
        CreateQuotationRequest effective = command.request();
        User caller = command.caller();

        if (caller != null && caller.getRole() == UserRole.SELLER && effective.sellerId() == null) {
            effective = new CreateQuotationRequest(
                    effective.customerId(),
                    caller.getId(),
                    effective.title(),
                    effective.destination(),
                    effective.description(),
                    effective.totalAmount(),
                    effective.currency(),
                    effective.status(),
                    effective.validUntil(),
                    effective.travelStartDate(),
                    effective.travelEndDate(),
                    effective.details(),
                    effective.tags(),
                    effective.priority(),
                    effective.assignee(),
                    effective.internalNotes(),
                    effective.creationSource(),
                    effective.publicSubmissionId());
        }

        return quotationService.create(effective, caller);
    }
}
