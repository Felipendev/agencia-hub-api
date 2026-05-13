package com.agenciahub.api.controller;

import com.agenciahub.api.domain.QuotationCreationSource;
import com.agenciahub.api.domain.QuotationStatus;
import com.agenciahub.api.domain.UserRole;
import com.agenciahub.api.dto.quotation.CreateQuotationRequest;
import com.agenciahub.api.dto.quotation.QuotationResponse;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.service.QuotationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = QuotationController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
@Import(GlobalExceptionHandler.class)
class QuotationControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private QuotationService quotationService;

    @Test
    void list_returnsQuotations() throws Exception {
        when(quotationService.search(isNull(), isNull(), isNull(), isNull(), eq(UserRole.OWNER)))
                .thenReturn(List.of());

        mockMvc.perform(get("/quotations"))
                .andExpect(status().isOk())
                .andExpect(content().string("[]"));
    }

    @Test
    void get_whenMissing_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(quotationService.getById(id)).thenThrow(new ResourceNotFoundException("Cotação não encontrada"));

        mockMvc.perform(get("/quotations/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void create_happyPath_returns201() throws Exception {
        UUID cid = UUID.randomUUID();
        UUID qid = UUID.randomUUID();
        var emptyDetails = objectMapper.createObjectNode();
        var resp = new QuotationResponse(
                qid,
                cid,
                "Cliente",
                null,
                null,
                null,
                null,
                "Título",
                "Destino",
                null,
                BigDecimal.ZERO,
                "BRL",
                QuotationStatus.DRAFT,
                LocalDate.parse("2026-12-31"),
                null,
                null,
                emptyDetails,
                Collections.emptyList(),
                false,
                null,
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"),
                null,
                QuotationCreationSource.INTERNAL,
                null,
                null,
                null);

        when(quotationService.create(any(CreateQuotationRequest.class), any())).thenReturn(resp);

        mockMvc.perform(post("/quotations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "%s",
                                  "title": "Orçamento",
                                  "destination": "Lisboa",
                                  "totalAmount": 100.00,
                                  "validUntil": "2026-12-31"
                                }
                                """.formatted(cid)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(qid.toString()));
    }

    @Test
    void create_invalidBody_returns400() throws Exception {
        mockMvc.perform(post("/quotations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
