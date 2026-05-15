package com.agenciahub.api.application.usecases.quotation.createquotation;

import com.agenciahub.api.application.usecases.quotation.QuotationSupport;
import com.agenciahub.api.application.usecases.quotation.QuotationResponseMapper;
import com.agenciahub.api.domain.QuotationCreationSource;
import com.agenciahub.api.domain.QuotationStatus;
import com.agenciahub.api.domain.UserRole;
import com.agenciahub.api.dto.quotation.CreateQuotationRequest;
import com.agenciahub.api.dto.quotation.QuotationResponse;
import com.agenciahub.api.entity.Customer;
import com.agenciahub.api.entity.Quotation;
import com.agenciahub.api.entity.SolicitacaoSubmission;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.CustomerRepository;
import com.agenciahub.api.repository.QuotationRepository;
import com.agenciahub.api.repository.SolicitacaoSubmissionRepository;
import com.agenciahub.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateQuotation implements CreateQuotationUseCase {

    private final QuotationRepository quotationRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final SolicitacaoSubmissionRepository solicitacaoSubmissionRepository;
    private final QuotationResponseMapper quotationResponseMapper;

    @Override
    public QuotationResponse execute(CreateQuotationCommand command) {
        CreateQuotationRequest request = command.request();
        User caller = command.caller();

        if (caller != null && caller.getRole() == UserRole.SELLER && request.sellerId() == null) {
            request = new CreateQuotationRequest(
                    request.customerId(),
                    caller.getId(),
                    request.title(),
                    request.destination(),
                    request.description(),
                    request.totalAmount(),
                    request.currency(),
                    request.status(),
                    request.validUntil(),
                    request.travelStartDate(),
                    request.travelEndDate(),
                    request.details(),
                    request.tags(),
                    request.priority(),
                    request.assignee(),
                    request.internalNotes(),
                    request.creationSource(),
                    request.publicSubmissionId());
        }

        final CreateQuotationRequest effective = request;

        Customer customer = customerRepository
                .findById(effective.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("cliente não encontrado: " + effective.customerId()));

        UUID agencyId = customer.getAgency().getId();
        User seller = QuotationSupport.resolveSellerInAgency(userRepository, effective.sellerId(), agencyId);

        SolicitacaoSubmission publicSub = null;
        if (effective.publicSubmissionId() != null) {
            publicSub = solicitacaoSubmissionRepository
                    .findById(effective.publicSubmissionId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "submissão pública não encontrada: " + effective.publicSubmissionId()));
            if (publicSub.getAgency() == null || !publicSub.getAgency().getId().equals(agencyId)) {
                throw new IllegalArgumentException("submissão pública não pertence a esta agência.");
            }
        }

        QuotationCreationSource source = effective.creationSource() != null
                ? effective.creationSource()
                : (publicSub != null ? QuotationCreationSource.PUBLIC_FORM : QuotationCreationSource.INTERNAL);

        String description = effective.description() != null ? effective.description() : "";
        String currency = (effective.currency() != null && !effective.currency().isBlank())
                ? effective.currency().strip().toUpperCase()
                : "BRL";
        QuotationStatus init = effective.status() != null ? effective.status() : QuotationStatus.DRAFT;
        List<String> tags = effective.tags() == null ? new ArrayList<>() : new ArrayList<>(effective.tags());
        boolean priority = Boolean.TRUE.equals(effective.priority());
        String notes = effective.internalNotes() != null ? effective.internalNotes().strip() : "";

        Quotation entity = Quotation.builder()
                .agency(customer.getAgency())
                .customer(customer)
                .seller(seller)
                .title(effective.title().strip())
                .destination(effective.destination().strip())
                .description(description)
                .totalAmount(effective.totalAmount())
                .currency(currency)
                .status(init)
                .validUntil(effective.validUntil())
                .travelStartDate(effective.travelStartDate())
                .travelEndDate(effective.travelEndDate())
                .detailsJson(effective.details())
                .tags(tags)
                .priority(priority)
                .assignee(QuotationSupport.blankToNull(effective.assignee()))
                .internalNotes(notes)
                .creationSource(source)
                .createdByUser(caller)
                .publicSubmission(publicSub)
                .build();

        return quotationResponseMapper.toResponse(quotationRepository.save(entity));
    }
}
