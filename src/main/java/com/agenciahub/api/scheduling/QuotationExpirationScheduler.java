package com.agenciahub.api.scheduling;

import com.agenciahub.api.application.integrations.email.EmailService;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.application.persistence.entity.Quotation;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.application.persistence.repository.QuotationRepository;
import com.agenciahub.api.domain.QuotationStatus;
import com.agenciahub.api.domain.enums.AccountKind;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * NOTIF-01: marks overdue quotations as EXPIRED and sends notification emails.
 * Also notifies about quotations expiring in 7 days.
 *
 * <p>Datas e o disparo do cron usam explicitamente {@link #AGENCY_ZONE} (o produto é
 * BR-only). Sem isso, {@code LocalDate.now()} usaria o fuso padrão da JVM/host (em Railway,
 * tipicamente UTC) — nas ~3h finais do dia em Brasília isso já é "amanhã" em UTC, adiantando
 * em um dia tanto a expiração quanto o alvo do aviso de "vence em 7 dias".</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuotationExpirationScheduler {

    private static final ZoneId AGENCY_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final List<QuotationStatus> ACTIVE_STATUSES =
            List.of(QuotationStatus.SENT, QuotationStatus.AWAITING_CLIENT);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final QuotationRepository quotationRepository;
    private final PlatformAccountRepository accountRepository;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 8 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void processExpired() {
        LocalDate today = LocalDate.now(AGENCY_ZONE);
        List<Quotation> expired = quotationRepository.findExpired(ACTIVE_STATUSES, today);
        for (Quotation q : expired) {
            q.setStatus(QuotationStatus.EXPIRED);
            quotationRepository.save(q);
            notifyVencida(q);
        }
        if (!expired.isEmpty()) {
            log.info("Marked {} quotations as EXPIRED", expired.size());
        }
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void notifyExpiringSoon() {
        LocalDate today = LocalDate.now(AGENCY_ZONE);
        LocalDate in7Days = today.plusDays(7);
        List<Quotation> expiringSoon = quotationRepository.findByStatusInAndValidUntil(ACTIVE_STATUSES, in7Days);
        int sent = 0;
        for (Quotation q : expiringSoon) {
            // Não avisar "vence em breve" para uma viagem cujo período já passou —
            // o titular não tem mais o que decidir sobre uma data que já ocorreu.
            if (q.getTravelEndDate() != null && q.getTravelEndDate().isBefore(today)) {
                log.info("Skipping expiring-soon notification for quotation {}: travel already in the past", q.getId());
                continue;
            }
            notifyVencendo(q);
            sent++;
        }
        if (sent > 0) {
            log.info("Sent expiring-soon notifications for {} quotations", sent);
        }
    }

    private void notifyVencida(Quotation q) {
        String title = q.getTitle();
        String validUntil = q.getValidUntil() != null ? q.getValidUntil().format(DATE_FMT) : "";
        sendToResponsibles(q, title, validUntil, false);
    }

    private void notifyVencendo(Quotation q) {
        String title = q.getTitle();
        String validUntil = q.getValidUntil() != null ? q.getValidUntil().format(DATE_FMT) : "";
        sendToResponsibles(q, title, validUntil, true);
    }

    private void sendToResponsibles(Quotation q, String title, String validUntil, boolean expiringSoon) {
        List<PlatformAccount> owners = accountRepository
                .findByAgency_IdAndAccountKindAndActiveTrue(q.getAgency().getId(), AccountKind.AGENCY_OWNER);
        for (PlatformAccount owner : owners) {
            boolean notif = expiringSoon
                    ? Boolean.TRUE.equals(owner.getNotifEmailCotacaoVencendo())
                    : Boolean.TRUE.equals(owner.getNotifEmailCotacaoVencida());
            if (notif) {
                emailService.sendQuotationExpiringSoon(owner.getEmail(), title, validUntil);
            }
        }
        PlatformAccount seller = q.getSeller();
        if (seller != null) {
            boolean notif = expiringSoon
                    ? Boolean.TRUE.equals(seller.getNotifEmailCotacaoVencendo())
                    : Boolean.TRUE.equals(seller.getNotifEmailCotacaoVencida());
            if (notif) {
                emailService.sendQuotationExpiringSoon(seller.getEmail(), title, validUntil);
            }
        }
    }
}
