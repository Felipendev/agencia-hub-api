package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.financial.createfinancialentry.CreateFinancialEntryUseCase;
import com.agenciahub.api.application.usecases.financial.getfinancialentrybyid.GetFinancialEntryByIdUseCase;
import com.agenciahub.api.application.usecases.financial.listfinancialentries.ListFinancialEntriesQuery;
import com.agenciahub.api.application.usecases.financial.listfinancialentries.ListFinancialEntriesUseCase;
import com.agenciahub.api.application.usecases.financial.updatefinancialentry.UpdateFinancialEntryUseCase;
import com.agenciahub.api.web.GlobalExceptionHandler;
import com.agenciahub.api.domain.FinancialEntryCategory;
import com.agenciahub.api.domain.FinancialEntryStatus;
import com.agenciahub.api.domain.FinancialEntryType;
import com.agenciahub.api.dto.financial.CreateFinancialEntryRequest;
import com.agenciahub.api.dto.financial.FinancialEntryResponse;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.security.JwtAuthFilter;
import com.agenciahub.api.security.RateLimitFilter;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = FinancialEntryController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class FinancialEntryControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    @MockitoBean
    private ListFinancialEntriesUseCase listFinancialEntriesUseCase;

    @MockitoBean
    private GetFinancialEntryByIdUseCase getFinancialEntryByIdUseCase;

    @MockitoBean
    private CreateFinancialEntryUseCase createFinancialEntryUseCase;

    @MockitoBean
    private UpdateFinancialEntryUseCase updateFinancialEntryUseCase;

    @Test
    void list_returnsEntries() throws Exception {
        UUID id = UUID.randomUUID();
        when(listFinancialEntriesUseCase.execute(any(ListFinancialEntriesQuery.class)))
                .thenReturn(List.of(new FinancialEntryResponse(
                        id,
                        "Pagamento fornecedor",
                        FinancialEntryType.EXPENSE,
                        FinancialEntryCategory.OPERATIONAL,
                        new BigDecimal("150.00"),
                        LocalDate.parse("2026-01-15"),
                        FinancialEntryStatus.CONFIRMED,
                        null,
                        null,
                        null)));

        mockMvc.perform(get("/financial-entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Pagamento fornecedor"));
    }

    @Test
    void get_whenMissing_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(getFinancialEntryByIdUseCase.execute(eq(id)))
                .thenThrow(new ResourceNotFoundException("lançamento financeiro não encontrado"));

        mockMvc.perform(get("/financial-entries/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void create_invalidBody_returns400() throws Exception {
        mockMvc.perform(post("/financial-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
