package com.agenciahub.api.integration.http;

import com.agenciahub.api.application.integrations.email.EmailService;
import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.application.persistence.entity.SolicitacaoSubmission;
import com.agenciahub.api.application.persistence.repository.AgencyRepository;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.application.persistence.repository.SolicitacaoSubmissionRepository;
import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.support.AbstractIntegrationTest;
import com.agenciahub.api.support.IntegrationHttpSupport;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * NOTIF-01 / UX-02: email notifications for quotation approval, account deletion, and referral seller import.
 */
class NotificationEmailIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EmailService emailService;

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private PlatformAccountRepository userRepository;

    @Autowired
    private SolicitacaoSubmissionRepository submissionRepository;

    @BeforeEach
    void resetMock() {
        Mockito.reset(emailService);
    }

    @AfterEach
    void restoreDeletionPendingAgencies() {
        List<Agency> pending = agencyRepository.findAll().stream()
                .filter(a -> a.getStatus() == AgencyStatus.DELETION_PENDING)
                .toList();
        for (Agency a : pending) {
            AgencyStatus prev = a.getStatusBeforeDeletion() != null
                    ? AgencyStatus.valueOf(a.getStatusBeforeDeletion())
                    : AgencyStatus.TRIAL;
            a.setStatus(prev);
            a.setDeletionScheduledAt(null);
            a.setStatusBeforeDeletion(null);
            agencyRepository.save(a);
        }
    }

    @Test
    void quotationAccepted_sendsEmailToOwner() throws Exception {
        UUID customerId = createCustomerId();
        UUID quotationId = createQuotationId(customerId);

        mockMvc.perform(http.authorized(mockMvc,
                        patch(IntegrationHttpSupport.API_PREFIX + "/quotations/" + quotationId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"status": "ACCEPTED"}
                                        """)))
                .andExpect(status().isOk());

        verify(emailService, atLeastOnce()).sendQuotationAccepted(
                anyString(), anyString(), anyString());
    }

    @Test
    void quotationNotAccepted_doesNotSendEmail() throws Exception {
        UUID customerId = createCustomerId();
        UUID quotationId = createQuotationId(customerId);

        mockMvc.perform(http.authorized(mockMvc,
                        patch(IntegrationHttpSupport.API_PREFIX + "/quotations/" + quotationId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"status": "SENT"}
                                        """)))
                .andExpect(status().isOk());

        Mockito.verifyNoMoreInteractions(emailService);
    }

    @Test
    void requestDeletion_sendsScheduledDeletionEmail() throws Exception {
        String token = http.loginOwner(mockMvc);

        mockMvc.perform(delete(IntegrationHttpSupport.API_PREFIX + "/auth/account")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        verify(emailService, atLeastOnce()).sendDeletionScheduled(anyString(), anyString());
    }

    @Test
    void createQuotationFromSubmissionWithReferralSeller_sendsEmailToSeller() throws Exception {
        PlatformAccount seedOwner = userRepository.findByEmail(IntegrationHttpSupport.SEED_OWNER_EMAIL).orElseThrow();
        UUID submissionId = createSubmissionWithSeller(seedOwner);

        UUID customerId = createCustomerId();
        mockMvc.perform(http.authorized(mockMvc,
                        post(IntegrationHttpSupport.API_PREFIX + "/quotations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"customerId":"%s","title":"Cotação UX02","destination":"Japão",
                                         "totalAmount":8000.00,"validUntil":"2027-12-31",
                                         "publicSubmissionId":"%s"}
                                        """.formatted(customerId, submissionId))))
                .andExpect(status().isCreated());

        verify(emailService, atLeastOnce()).sendNewSubmissionAlert(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void createQuotationFromSubmissionWithoutReferralSeller_doesNotSendEmailToSeller() throws Exception {
        UUID submissionId = createSubmissionWithoutSeller(
                userRepository.findByEmail(IntegrationHttpSupport.SEED_OWNER_EMAIL).orElseThrow().getAgency());

        UUID customerId = createCustomerId();
        mockMvc.perform(http.authorized(mockMvc,
                        post(IntegrationHttpSupport.API_PREFIX + "/quotations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"customerId":"%s","title":"Cotação UX02 sem seller","destination":"Roma",
                                         "totalAmount":3000.00,"validUntil":"2027-12-31",
                                         "publicSubmissionId":"%s"}
                                        """.formatted(customerId, submissionId))))
                .andExpect(status().isCreated());

        verify(emailService, never()).sendNewSubmissionAlert(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private UUID createSubmissionWithSeller(PlatformAccount seller) {
        SolicitacaoSubmission submission = submissionRepository.save(SolicitacaoSubmission.builder()
                .agency(seller.getAgency())
                .referralSeller(seller)
                .slug("test-slug-ux02")
                .nome("Cliente Via Link")
                .email("via-link@test.com")
                .telefone("11999990000")
                .observacoes("Teste UX-02")
                .detalhes(JsonNodeFactory.instance.objectNode())
                .consentimentoLgpd(false)
                .build());
        return submission.getId();
    }

    private UUID createSubmissionWithoutSeller(com.agenciahub.api.application.persistence.entity.Agency agency) {
        SolicitacaoSubmission submission = submissionRepository.save(SolicitacaoSubmission.builder()
                .agency(agency)
                .referralSeller(null)
                .slug("test-slug-ux02-no-seller")
                .nome("Cliente Direto")
                .email("direto@test.com")
                .telefone("11988880000")
                .observacoes("Sem vendedor")
                .detalhes(JsonNodeFactory.instance.objectNode())
                .consentimentoLgpd(false)
                .build());
        return submission.getId();
    }

    private UUID createCustomerId() throws Exception {
        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 9);
        String response = mockMvc.perform(http.authorized(mockMvc,
                        post(IntegrationHttpSupport.API_PREFIX + "/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"name":"Cliente Notif","email":"notif-%s@test.com",
                                         "phone":"119%s","interestDestination":"Europa","status":"PROSPECT"}
                                        """.formatted(unique, unique))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(http.parse(response).get("id").asText());
    }

    private UUID createQuotationId(UUID customerId) throws Exception {
        String response = mockMvc.perform(http.authorized(mockMvc,
                        post(IntegrationHttpSupport.API_PREFIX + "/quotations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"customerId":"%s","title":"Viagem Notif","destination":"Paris",
                                         "totalAmount":5000.00,"validUntil":"2027-12-31"}
                                        """.formatted(customerId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(http.parse(response).get("id").asText());
    }
}
