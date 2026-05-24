package com.agenciahub.api.application.usecases.agency.delete;

import com.agenciahub.api.application.integrations.email.EmailService;
import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.application.persistence.repository.AgencyRepository;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class RequestAccountDeletion {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.of("America/Sao_Paulo"));

    private final PlatformAccountRepository platformAccountRepository;
    private final AgencyRepository agencyRepository;
    private final EmailService emailService;

    @Transactional
    public void execute(RequestAccountDeletionRequestDTO request) {
        PlatformAccount owner = platformAccountRepository.findByIdWithAgency(request.ownerId())
                .orElseThrow(() -> new ResourceNotFoundException("usuário não encontrado"));

        if (owner.getAccountKind() != AccountKind.AGENCY_OWNER) {
            throw new IllegalStateException("apenas o dono da agência pode solicitar a exclusão da conta.");
        }

        Agency agency = owner.getAgency();
        if (agency == null) {
            throw new ResourceNotFoundException("agência não encontrada");
        }

        if (agency.getStatus() == AgencyStatus.DELETION_PENDING) {
            throw new IllegalStateException("conta já está em processo de exclusão");
        }

        agency.setStatusBeforeDeletion(agency.getStatus().name());
        agency.setStatus(AgencyStatus.DELETION_PENDING);
        agency.setDeletionScheduledAt(Instant.now().plus(7, ChronoUnit.DAYS));

        agencyRepository.save(agency);
        platformAccountRepository.updateLastLogoutAtByAgencyId(agency.getId(), Instant.now());

        if (Boolean.TRUE.equals(owner.getNotifEmailExclusaoAgendada())) {
            emailService.sendDeletionScheduled(
                    owner.getEmail(), DATE_FMT.format(agency.getDeletionScheduledAt()));
        }
    }
}
