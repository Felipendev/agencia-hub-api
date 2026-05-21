package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.quotation.create.CreateQuotationCommand;
import com.agenciahub.api.application.usecases.quotation.create.CreateQuotationUseCase;
import com.agenciahub.api.application.usecases.quotation.update.UpdateQuotationCommand;
import com.agenciahub.api.application.usecases.quotation.retrieve.byid.GetQuotationByIdUseCase;
import com.agenciahub.api.application.usecases.quotation.retrieve.list.ListQuotationsQuery;
import com.agenciahub.api.application.usecases.quotation.retrieve.list.ListQuotationsUseCase;
import com.agenciahub.api.application.usecases.quotation.delete.DeleteQuotationUseCase;
import com.agenciahub.api.application.usecases.quotation.update.UpdateQuotationUseCase;
import com.agenciahub.api.domain.QuotationCreationSource;
import com.agenciahub.api.domain.QuotationStatus;
import com.agenciahub.api.application.usecases.quotation.shared.QuotationSummaryResponseDTO;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.security.JwtAuthFilter;
import com.agenciahub.api.security.RateLimitFilter;
import com.agenciahub.api.support.WebMvcControllerTestImports;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = QuotationController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcControllerTestImports.class)
class QuotationControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    @MockitoBean
    private ListQuotationsUseCase listQuotationsUseCase;

    @MockitoBean
    private GetQuotationByIdUseCase getQuotationByIdUseCase;

    @MockitoBean
    private CreateQuotationUseCase createQuotationUseCase;

    @MockitoBean
    private UpdateQuotationUseCase updateQuotationUseCase;

    @MockitoBean
    private DeleteQuotationUseCase deleteQuotationUseCase;

    @Test
    void list_returnsQuotations() throws Exception {
        when(listQuotationsUseCase.execute(any(ListQuotationsQuery.class)))
                .thenAnswer(inv -> {
                    ListQuotationsQuery q = inv.getArgument(0);
                    assertNull(q.caller());
                    return List.<QuotationSummaryResponseDTO>of();
                });

        mockMvc.perform(get("/quotations"))
                .andExpect(status().isOk())
                .andExpect(content().string("[]"));
    }

    @Test
    void get_whenMissing_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(getQuotationByIdUseCase.execute(eq(id)))
                .thenThrow(new ResourceNotFoundException("cotação não encontrada"));

        mockMvc.perform(get("/quotations/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void create_happyPath_returns201() throws Exception {
        UUID cid = UUID.randomUUID();
        UUID qid = UUID.randomUUID();
        var emptyDetails = objectMapper.createObjectNode();
        var resp = new QuotationSummaryResponseDTO(
                qid,
                cid,
                "Cliente",
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
                QuotationCreationSource.INTERNAL,
                null,
                null,
                null);

        when(createQuotationUseCase.execute(any(CreateQuotationCommand.class)))
                .thenAnswer(inv -> {
                    CreateQuotationCommand cmd = inv.getArgument(0);
                    assertNull(cmd.caller());
                    assertEquals(cid, cmd.request().customerId());
                    return resp;
                });

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

    @Test
    void create_negativeTotalAmount_returns400() throws Exception {
        UUID cid = UUID.randomUUID();
        mockMvc.perform(post("/quotations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "%s",
                                  "title": "Orçamento",
                                  "destination": "Lisboa",
                                  "totalAmount": -1.00,
                                  "validUntil": "2026-12-31"
                                }
                                """.formatted(cid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void get_happyPath_returns200() throws Exception {
        UUID qid = UUID.randomUUID();
        UUID cid = UUID.randomUUID();
        when(getQuotationByIdUseCase.execute(eq(qid))).thenReturn(sampleDto(qid, cid));

        mockMvc.perform(get("/quotations/" + qid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(qid.toString()))
                .andExpect(jsonPath("$.totalAmount").value(10));
    }

    @Test
    void update_happyPath_returns200() throws Exception {
        UUID qid = UUID.randomUUID();
        UUID cid = UUID.randomUUID();
        when(updateQuotationUseCase.execute(any(UpdateQuotationCommand.class)))
                .thenReturn(sampleDto(qid, cid));

        mockMvc.perform(patch("/quotations/" + qid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Novo Título\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(qid.toString()));
    }

    @Test
    void update_whenMissing_returns404() throws Exception {
        UUID qid = UUID.randomUUID();
        when(updateQuotationUseCase.execute(any(UpdateQuotationCommand.class)))
                .thenThrow(new ResourceNotFoundException("cotação não encontrada"));

        mockMvc.perform(patch("/quotations/" + qid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"X\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void delete_happyPath_returns204() throws Exception {
        UUID qid = UUID.randomUUID();
        doNothing().when(deleteQuotationUseCase).execute(eq(qid));

        mockMvc.perform(delete("/quotations/" + qid))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_whenMissing_returns404() throws Exception {
        UUID qid = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("cotação não encontrada"))
                .when(deleteQuotationUseCase).execute(eq(qid));

        mockMvc.perform(delete("/quotations/" + qid))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    private QuotationSummaryResponseDTO sampleDto(UUID qid, UUID cid) {
        return new QuotationSummaryResponseDTO(
                qid, cid, "Cliente", null, null,
                "Título", "Destino", null,
                BigDecimal.TEN, "BRL",
                QuotationStatus.DRAFT,
                LocalDate.parse("2026-12-31"),
                null, null,
                objectMapper.createObjectNode(),
                Collections.emptyList(),
                false, null, null,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"),
                QuotationCreationSource.INTERNAL,
                null, null, null);
    }
}
