package com.agenciahub.api.integration.http;

import com.agenciahub.api.application.integrations.email.EmailService;
import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.repository.AgencyRepository;
import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.support.AbstractIntegrationTest;
import com.agenciahub.api.support.IntegrationHttpSupport;
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
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * NOTIF-01: email notifications for quotation approval and account deletion scheduling.
 */
class NotificationEmailIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EmailService emailService;

    @Autowired
    private AgencyRepository agencyRepository;

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

    // ── Helpers ──────────────────────────────────────────────────────────────

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
