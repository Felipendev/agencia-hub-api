package com.agenciahub.api.application.usecases.datadeletion;

import com.agenciahub.api.application.integrations.email.EmailService;
import com.agenciahub.api.application.persistence.entity.DataDeletionRequest;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.application.persistence.entity.SolicitacaoSubmission;
import com.agenciahub.api.application.persistence.repository.DataDeletionRequestRepository;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.application.persistence.repository.SolicitacaoSubmissionRepository;
import com.agenciahub.api.domain.DataDeletionStatus;
import com.agenciahub.api.domain.enums.AccountKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateDataDeletionRequest {

    private final DataDeletionRequestRepository repository;
    private final SolicitacaoSubmissionRepository submissionRepository;
    private final PlatformAccountRepository accountRepository;
    private final EmailService emailService;

    @Transactional
    public CreateDataDeletionResult execute(CreateDataDeletionRequestDTO dto) {
        Instant startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS);

        Optional<DataDeletionRequest> existing = repository.findFirstByEmailIgnoreCaseAndStatusAndCreatedAtAfter(
                dto.email(), DataDeletionStatus.PENDING, startOfDay);

        if (existing.isPresent()) {
            return new CreateDataDeletionResult(existing.get().getId(), false);
        }

        DataDeletionRequest request = repository.save(DataDeletionRequest.builder()
                .email(dto.email().trim())
                .telefone(dto.telefone() != null ? dto.telefone().trim() : null)
                .motivo(dto.motivo() != null ? dto.motivo().trim() : null)
                .status(DataDeletionStatus.PENDING)
                .build());

        emailService.sendDataDeletionRequestConfirmation(dto.email().trim());
        notifyOwners(request.getId(), dto.email().trim());

        return new CreateDataDeletionResult(request.getId(), true);
    }

    private void notifyOwners(UUID requestId, String email) {
        Set<UUID> agencyIds = new HashSet<>();
        for (SolicitacaoSubmission s : submissionRepository.findByEmailIgnoreCase(email)) {
            if (s.getAgency() != null) agencyIds.add(s.getAgency().getId());
        }
        for (UUID agencyId : agencyIds) {
            accountRepository.findByAgency_IdAndAccountKindAndActiveTrue(agencyId, AccountKind.AGENCY_OWNER)
                    .forEach(owner ->
                            emailService.sendDataDeletionOwnerNotification(owner.getEmail(), requestId.toString()));
        }
    }

    public record CreateDataDeletionResult(UUID requestId, boolean created) {}
}
