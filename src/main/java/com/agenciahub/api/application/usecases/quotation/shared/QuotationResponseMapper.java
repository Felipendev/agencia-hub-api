package com.agenciahub.api.application.usecases.quotation.shared;

import com.agenciahub.api.domain.QuotationCreationSource;
import com.agenciahub.api.application.usecases.quotation.shared.QuotationSummaryResponseDTO;
import com.agenciahub.api.entity.Customer;
import com.agenciahub.api.entity.Quotation;
import com.agenciahub.api.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class QuotationResponseMapper {

    public QuotationSummaryResponseDTO toResponse(Quotation q) {
        Customer c = q.getCustomer();
        User s = q.getSeller();
        User createdBy = q.getCreatedByUser();
        QuotationCreationSource src = q.getCreationSource() != null
                ? q.getCreationSource()
                : QuotationCreationSource.INTERNAL;
        UUID pubId = q.getPublicSubmission() != null ? q.getPublicSubmission().getId() : null;
        return new QuotationSummaryResponseDTO(
                q.getId(),
                c.getId(),
                c.getName(),
                s != null ? s.getId() : null,
                s != null ? s.getName() : null,
                q.getTitle(),
                q.getDestination(),
                q.getDescription(),
                q.getTotalAmount(),
                q.getCurrency(),
                q.getStatus(),
                q.getValidUntil(),
                q.getTravelStartDate(),
                q.getTravelEndDate(),
                q.getDetailsJson(),
                q.getTags() != null ? List.copyOf(q.getTags()) : List.of(),
                Boolean.TRUE.equals(q.getPriority()),
                q.getAssignee(),
                q.getInternalNotes() != null ? q.getInternalNotes() : "",
                q.getCreatedAt(),
                q.getUpdatedAt(),
                src,
                createdBy != null ? createdBy.getId() : null,
                createdBy != null ? createdBy.getName() : null,
                pubId);
    }
}
