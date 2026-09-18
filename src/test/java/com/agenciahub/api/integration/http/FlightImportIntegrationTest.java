package com.agenciahub.api.integration.http;

import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.repository.AgencyRepository;
import com.agenciahub.api.application.services.flights.*;
import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.domain.SubscriptionStatus;
import com.agenciahub.api.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static com.agenciahub.api.support.IntegrationHttpSupport.unauthenticated;

/** Real PostgreSQL quota/cache transactions; vision is mocked, no paid calls. */
class FlightImportIntegrationTest extends AbstractIntegrationTest {
    @Autowired AgencyRepository agencies;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager manager;
    @Autowired ObjectMapper mapper;

    private UUID agency() {
        return agencies.save(Agency.builder().name("Flight test " + UUID.randomUUID()).status(AgencyStatus.ACTIVE)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build()).getId();
    }
    private GeminiFlightReader mockVision() throws Exception {
        GeminiFlightReader vision = mock(GeminiFlightReader.class);
        when(vision.configured()).thenReturn(true);
        when(vision.read(any())).thenReturn(new GeminiFlightReader.Reading(
                new FlightExtraction(List.of(), List.of("Teste sem ofertas")), 1000, 100, new BigDecimal("0.000400")));
        return vision;
    }
    private FlightImportService service(GeminiFlightReader vision, int requests) throws Exception {
        var documents = mock(FlightDocumentReader.class);
        when(documents.read(any())).thenReturn(new FlightDocumentReader.Document("image/png", new byte[]{1}, null, null));
        var config = new FlightImportProperties(); config.setMonthlyRequests(requests);
        return new FlightImportService(jdbc, manager, mapper, documents, vision, config);
    }
    @Test void cacheIsReusedOnlyWithinSameAgency() throws Exception {
        var vision = mockVision(); var service = service(vision, 10);
        UUID a = agency(), b = agency(), user = UUID.randomUUID();
        var first = service.extract(a, user, "oferta.png", new byte[]{1});
        var cached = service.extract(a, user, "renomeada.png", new byte[]{1});
        var other = service.extract(b, user, "oferta.png", new byte[]{1});
        assertThat(cached.id()).isEqualTo(first.id()); assertThat(cached.cached()).isTrue();
        assertThat(other.id()).isNotEqualTo(first.id()); assertThat(other.cached()).isFalse();
        verify(vision, times(2)).read(any());
    }
    @Test void concurrentUploadsCannotExceedMonthlyRequestLimit() throws Exception {
        var vision = mockVision(); var service = service(vision, 1); UUID agency = agency();
        var start = new CountDownLatch(1); var pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                byte value = (byte) i;
                futures.add(pool.submit(() -> {
                    start.await(5, TimeUnit.SECONDS);
                    try { service.extract(agency, UUID.randomUUID(), "oferta.png", new byte[]{value}); return true; }
                    catch (IllegalStateException e) { assertThat(e.getMessage()).contains("Limite mensal"); return false; }
                }));
            }
            start.countDown();
            int success = 0; for (var future : futures) if (future.get(10, TimeUnit.SECONDS)) success++;
            assertThat(success).isEqualTo(1); verify(vision, times(1)).read(any());
            assertThat(jdbc.queryForObject("SELECT attempts FROM flight_import_usage WHERE agency_id=?", Integer.class, agency)).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }
    @Test void uploadRequiresAuthentication() throws Exception {
        mockMvc.perform(multipart("/flight-imports").file("file", new byte[]{1})).andExpect(unauthenticated());
    }
}
