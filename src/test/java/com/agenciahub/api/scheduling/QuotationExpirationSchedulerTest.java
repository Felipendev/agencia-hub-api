package com.agenciahub.api.scheduling;

import com.agenciahub.api.application.integrations.email.EmailService;
import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.entity.Quotation;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.application.persistence.repository.QuotationRepository;
import com.agenciahub.api.domain.QuotationStatus;
import com.agenciahub.api.domain.enums.AccountKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TODO-017: o aviso de "cotação vence em breve" não deve ser enviado quando a viagem em questão
 * já ocorreu — não faz sentido avisar sobre prazo de decisão de uma viagem passada.
 */
@ExtendWith(MockitoExtension.class)
class QuotationExpirationSchedulerTest {

    private static final ZoneId AGENCY_ZONE = ZoneId.of("America/Sao_Paulo");

    @Mock private QuotationRepository quotationRepository;
    @Mock private PlatformAccountRepository accountRepository;
    @Mock private EmailService emailService;

    @Test
    void skipsNotificationWhenTravelAlreadyHappened() {
        LocalDate today = LocalDate.now(AGENCY_ZONE);
        Agency agency = Agency.builder().id(UUID.randomUUID()).build();

        Quotation pastTrip = Quotation.builder()
                .id(UUID.randomUUID()).agency(agency).title("Viagem passada")
                .validUntil(today.plusDays(7)).travelEndDate(today.minusDays(10))
                .status(QuotationStatus.SENT).build();
        Quotation futureTrip = Quotation.builder()
                .id(UUID.randomUUID()).agency(agency).title("Viagem futura")
                .validUntil(today.plusDays(7)).travelEndDate(today.plusDays(10))
                .status(QuotationStatus.SENT).build();

        when(quotationRepository.findByStatusInAndValidUntil(anyList(), any()))
                .thenReturn(List.of(pastTrip, futureTrip));
        when(accountRepository.findByAgency_IdAndAccountKindAndActiveTrue(any(), any()))
                .thenReturn(List.of());

        QuotationExpirationScheduler scheduler =
                new QuotationExpirationScheduler(quotationRepository, accountRepository, emailService);
        scheduler.notifyExpiringSoon();

        // Só a cotação com viagem futura deve gerar tentativa de notificação (mesmo sem
        // destinatários configurados aqui, sendToResponsibles é chamado só para ela).
        verify(accountRepository, times(1))
                .findByAgency_IdAndAccountKindAndActiveTrue(agency.getId(), AccountKind.AGENCY_OWNER);
        verify(emailService, never()).sendQuotationExpiringSoon(anyString(), anyString(), anyString());
    }
}
