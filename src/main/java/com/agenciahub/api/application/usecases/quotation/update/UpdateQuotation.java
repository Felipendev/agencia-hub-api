package com.agenciahub.api.application.usecases.quotation.update;

import com.agenciahub.api.application.usecases.quotation.shared.QuotationSupport;
import com.agenciahub.api.application.usecases.quotation.shared.QuotationResponseMapper;
import com.agenciahub.api.application.usecases.quotation.shared.QuotationSummaryResponseDTO;
import com.agenciahub.api.application.usecases.quotation.update.UpdateQuotationRequestDTO;
import com.agenciahub.api.application.persistence.entity.Quotation;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.application.persistence.repository.QuotationRepository;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class UpdateQuotation implements UpdateQuotationUseCase {

    private final QuotationRepository quotationRepository;
    private final PlatformAccountRepository userRepository;
    private final QuotationResponseMapper quotationResponseMapper;

    @Override
    @Transactional
    public QuotationSummaryResponseDTO execute(UpdateQuotationCommand command) {
        Quotation entity = quotationRepository
                .findById(command.id())
                .orElseThrow(() -> new ResourceNotFoundException("cotação não encontrada: " + command.id()));
        UpdateQuotationRequestDTO request = command.request();

        if (Boolean.TRUE.equals(request.unsetSeller())) {
            entity.setSeller(null);
        } else if (request.sellerId() != null) {
            entity.setSeller(QuotationSupport.resolveSellerInAgency(
                    userRepository, request.sellerId(), entity.getCustomer().getAgency().getId()));
        }
        if (request.title() != null) {
            entity.setTitle(request.title().strip());
        }
        if (request.destination() != null) {
            entity.setDestination(request.destination().strip());
        }
        if (request.description() != null) {
            entity.setDescription(request.description());
        }
        if (request.totalAmount() != null) {
            entity.setTotalAmount(request.totalAmount());
        }
        if (request.currency() != null) {
            entity.setCurrency(request.currency().strip().toUpperCase());
        }
        if (request.status() != null) {
            entity.setStatus(request.status());
        }
        if (request.validUntil() != null) {
            entity.setValidUntil(request.validUntil());
        }
        if (request.travelStartDate() != null) {
            entity.setTravelStartDate(request.travelStartDate());
        }
        if (request.travelEndDate() != null) {
            entity.setTravelEndDate(request.travelEndDate());
        }
        if (request.details() != null) {
            entity.setDetailsJson(request.details());
        }
        if (request.tags() != null) {
            entity.setTags(new ArrayList<>(request.tags()));
        }
        if (request.priority() != null) {
            entity.setPriority(request.priority());
        }
        if (request.assignee() != null) {
            entity.setAssignee(QuotationSupport.blankToNull(request.assignee()));
        }
        if (request.internalNotes() != null) {
            entity.setInternalNotes(request.internalNotes().strip());
        }

        return quotationResponseMapper.toResponse(quotationRepository.save(entity));
    }
}
