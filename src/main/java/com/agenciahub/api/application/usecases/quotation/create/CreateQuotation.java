package com.agenciahub.api.application.usecases.quotation.create;

import com.agenciahub.api.application.integrations.email.EmailService;
import com.agenciahub.api.application.usecases.quotation.shared.QuotationSupport;
import com.agenciahub.api.application.usecases.quotation.shared.QuotationResponseMapper;
import com.agenciahub.api.domain.QuotationCreationSource;
import com.agenciahub.api.domain.QuotationStatus;
import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.application.usecases.quotation.create.CreateQuotationRequestDTO;
import com.agenciahub.api.application.usecases.quotation.shared.QuotationSummaryResponseDTO;
import com.agenciahub.api.application.persistence.entity.CrmCustomer;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.application.persistence.entity.Quotation;
import com.agenciahub.api.application.persistence.entity.SolicitacaoSubmission;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.application.persistence.repository.CrmCustomerRepository;
import com.agenciahub.api.application.persistence.repository.QuotationRepository;
import com.agenciahub.api.application.persistence.repository.SolicitacaoSubmissionRepository;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateQuotation implements CreateQuotationUseCase {

    private final QuotationRepository quotationRepository;
    private final CrmCustomerRepository customerRepository;
    private final PlatformAccountRepository userRepository;
    private final SolicitacaoSubmissionRepository solicitacaoSubmissionRepository;
    private final QuotationResponseMapper quotationResponseMapper;
    private final EmailService emailService;
    private final com.agenciahub.api.application.usecases.quotation.shared.QuotationApprovalService approvalService;

    @Value("${app.base-url:http://localhost:3000}")
    private String appBaseUrl;

    @Override
    @Transactional
    public QuotationSummaryResponseDTO execute(CreateQuotationCommand command) {
        CreateQuotationRequestDTO request = command.request();
        PlatformAccount caller = command.caller();

        if (caller != null && caller.getAccountKind() == AccountKind.SALES_AGENT && request.sellerId() == null) {
            request = new CreateQuotationRequestDTO(
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

        final CreateQuotationRequestDTO effective = request;

        UUID agencyId = TenantContext.requireAgencyId();

        CrmCustomer customer = customerRepository
                .findById(effective.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("cliente não encontrado: " + effective.customerId()));

        if (!customer.getAgency().getId().equals(agencyId)) {
            throw new ResourceNotFoundException("cliente não encontrado: " + effective.customerId());
        }
        PlatformAccount seller = QuotationSupport.resolveSellerInAgency(userRepository, effective.sellerId(), agencyId);

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

        QuotationSummaryResponseDTO response = quotationResponseMapper.toResponse(quotationRepository.save(entity));
        approvalService.synchronize(entity);

        if (publicSub != null) {
            notifyReferralSeller(publicSub, entity);
        }

        return response;
    }

    private void notifyReferralSeller(SolicitacaoSubmission submission, Quotation quotation) {
        PlatformAccount referralSeller = submission.getReferralSeller();
        if (referralSeller == null || !Boolean.TRUE.equals(referralSeller.getNotifEmailSubmissao())) {
            return;
        }
        String agencyName = quotation.getAgency().getName();
        String dashboardUrl = appBaseUrl + "/minhas-comissoes";
        String datas = formatDates(quotation.getTravelStartDate(), quotation.getTravelEndDate());
        emailService.sendNewSubmissionAlert(
                referralSeller.getEmail(),
                agencyName,
                submission.getNome(),
                submission.getTelefone(),
                quotation.getDestination(),
                datas,
                dashboardUrl);
    }

    private String formatDates(LocalDate start, LocalDate end) {
        if (start == null) return "";
        return end != null ? start + " – " + end : start.toString();
    }
}
