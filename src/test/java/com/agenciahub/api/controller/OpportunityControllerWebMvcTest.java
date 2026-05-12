package com.agenciahub.api.controller;

import com.agenciahub.api.domain.OpportunityStatus;
import com.agenciahub.api.dto.opportunity.CreateOpportunityRequest;
import com.agenciahub.api.dto.opportunity.OpportunityResponse;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.service.OpportunityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = OpportunityController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
@Import(GlobalExceptionHandler.class)
class OpportunityControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OpportunityService opportunityService;

    @Test
    void list_returnsOpportunities() throws Exception {
        UUID id = UUID.randomUUID();
        when(opportunityService.list(null))
                .thenReturn(List.of(new OpportunityResponse(
                        id,
                        UUID.randomUUID(),
                        "Cliente",
                        "Título",
                        "Paris",
                        BigDecimal.valueOf(5000),
                        OpportunityStatus.NEW_LEAD,
                        LocalDate.parse("2026-06-01"),
                        null)));

        mockMvc.perform(get("/opportunities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Título"));
    }

    @Test
    void get_whenMissing_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(opportunityService.getById(id)).thenThrow(new ResourceNotFoundException("Oportunidade não encontrada"));

        mockMvc.perform(get("/opportunities/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_happyPath_returns201() throws Exception {
        UUID cid = UUID.randomUUID();
        UUID oid = UUID.randomUUID();
        when(opportunityService.create(any(CreateOpportunityRequest.class)))
                .thenReturn(new OpportunityResponse(
                        oid,
                        cid,
                        "Cliente",
                        "Título",
                        "Paris",
                        BigDecimal.valueOf(5000),
                        OpportunityStatus.NEW_LEAD,
                        LocalDate.parse("2026-06-01"),
                        null));

        mockMvc.perform(post("/opportunities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "%s",
                                  "title": "Título",
                                  "destination": "Paris",
                                  "estimatedAmount": 5000,
                                  "status": "NEW_LEAD",
                                  "expectedTravelDate": "2026-06-01"
                                }
                                """.formatted(cid)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(oid.toString()));
    }

    @Test
    void create_invalidBody_returns400() throws Exception {
        mockMvc.perform(post("/opportunities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
