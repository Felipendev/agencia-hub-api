package com.agenciahub.api.integration.http;

import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.entity.CrmCustomer;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.application.persistence.entity.Trip;
import com.agenciahub.api.application.persistence.repository.AgencyRepository;
import com.agenciahub.api.application.persistence.repository.CrmCustomerRepository;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.application.persistence.repository.TripRepository;
import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.domain.CustomerStatus;
import com.agenciahub.api.domain.SubscriptionStatus;
import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.support.AbstractIntegrationTest;
import com.agenciahub.api.support.IntegrationHttpSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Garante que /trips (viagens) respeita o isolamento por agência e cobre múltiplos trechos e busca por localizador. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TripMultiTenancyIntegrationTest extends AbstractIntegrationTest {
    @Autowired private AgencyRepository agencyRepository;
    @Autowired private PlatformAccountRepository accountRepository;
    @Autowired private CrmCustomerRepository customerRepository;
    @Autowired private TripRepository tripRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private UUID agencyBTripId;

    @BeforeAll
    void setupAgencyBTrip() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Agency agencyB = agencyRepository.save(Agency.builder()
                .name("Agency B trip " + suffix).status(AgencyStatus.ACTIVE)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build());
        accountRepository.save(PlatformAccount.builder()
                .agency(agencyB).name("Owner B").email("owner-b-trip-" + suffix + "@test.local")
                .passwordHash(passwordEncoder.encode("safe-password"))
                .accountKind(AccountKind.AGENCY_OWNER).active(true).emailVerified(true).termsAccepted(true).build());
        CrmCustomer customerB = customerRepository.save(CrmCustomer.builder()
                .agency(agencyB).name("Private B customer " + suffix)
                .interestDestination("Europa").status(CustomerStatus.PROSPECT).notes("").build());
        agencyBTripId = tripRepository.save(Trip.builder()
                .agency(agencyB).customer(customerB).serviceType("FLIGHT")
                .bookingLocator("PRIVATEB" + suffix).status("UPCOMING").build()).getId();
    }

    @Test
    void ownerA_cannotReadUpdateOrDeleteAgencyBTrip() throws Exception {
        mockMvc.perform(http.authorized(mockMvc, get(IntegrationHttpSupport.API_PREFIX + "/trips/" + agencyBTripId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(http.authorized(mockMvc, patch(IntegrationHttpSupport.API_PREFIX + "/trips/" + agencyBTripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + UUID.randomUUID() + "\",\"serviceType\":\"FLIGHT\"}")))
                .andExpect(status().isNotFound());
        mockMvc.perform(http.authorized(mockMvc, delete(IntegrationHttpSupport.API_PREFIX + "/trips/" + agencyBTripId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerA_listDoesNotContainAgencyBTrip() throws Exception {
        mockMvc.perform(http.authorized(mockMvc, get(IntegrationHttpSupport.API_PREFIX + "/trips")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(agencyBTripId)).isEmpty());
    }

    @Test
    void ownerA_locatorSearchDoesNotLeakAgencyBTrip() throws Exception {
        mockMvc.perform(http.authorized(mockMvc, get(IntegrationHttpSupport.API_PREFIX + "/trips")
                        .param("locator", "PRIVATEB")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(agencyBTripId)).isEmpty());
    }

    @Test
    void ownerA_createsTripWithMultipleSegmentsAndNoQuotation() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String customerResponse = mockMvc.perform(http.authorized(mockMvc, post(IntegrationHttpSupport.API_PREFIX + "/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cliente trip " + suffix + "\",\"status\":\"PROSPECT\"}")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID customerId = UUID.fromString(http.parse(customerResponse).get("id").asText());

        String locator = "MULTI" + suffix;
        String departure = Instant.now().plusSeconds(3600).toString();
        String arrival = Instant.now().plusSeconds(7200).toString();
        String body = """
                {
                  "customerId": "%s",
                  "serviceType": "FLIGHT",
                  "bookingLocator": "%s",
                  "status": "UPCOMING",
                  "travelStartDate": "%s",
                  "segments": [
                    {"origin": "GRU", "destination": "LIS", "departureAt": "%s", "flightNumber": "AA100"},
                    {"origin": "LIS", "destination": "GRU", "arrivalAt": "%s", "flightNumber": "AA200"}
                  ]
                }
                """.formatted(customerId, locator, LocalDate.now().plusDays(30), departure, arrival);

        mockMvc.perform(http.authorized(mockMvc, post(IntegrationHttpSupport.API_PREFIX + "/trips")
                        .contentType(MediaType.APPLICATION_JSON).content(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trip.bookingLocator").value(locator))
                .andExpect(jsonPath("$.quotationId").value(nullValue()))
                .andExpect(jsonPath("$.segments.length()").value(2))
                .andExpect(jsonPath("$.segments[0].segmentNumber").value(1))
                .andExpect(jsonPath("$.segments[1].segmentNumber").value(2));
    }
}
