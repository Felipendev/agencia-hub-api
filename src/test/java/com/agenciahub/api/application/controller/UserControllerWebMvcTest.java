package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.user.create.CreateUserUseCase;
import com.agenciahub.api.application.usecases.user.retrieve.byid.GetUserByIdUseCase;
import com.agenciahub.api.application.usecases.salesagent.retrieve.listactive.ListActiveSalesAgentsUseCase;
import com.agenciahub.api.application.usecases.user.retrieve.list.ListUsersUseCase;
import com.agenciahub.api.application.usecases.user.update.UpdateUserUseCase;
import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.application.usecases.user.shared.UserSummaryResponseDTO;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.security.JwtAuthFilter;
import com.agenciahub.api.security.RateLimitFilter;
import com.agenciahub.api.support.WebMvcControllerTestImports;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcControllerTestImports.class)
class UserControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    @MockitoBean
    private ListUsersUseCase listUsersUseCase;

    @MockitoBean
    private ListActiveSalesAgentsUseCase listActiveSalesAgentsUseCase;

    @MockitoBean
    private GetUserByIdUseCase getUserByIdUseCase;

    @MockitoBean
    private CreateUserUseCase createUserUseCase;

    @MockitoBean
    private UpdateUserUseCase updateUserUseCase;

    @Test
    void list_returnsUsers() throws Exception {
        UUID id = UUID.randomUUID();
        when(listUsersUseCase.execute(isNull()))
                .thenReturn(List.of(new UserSummaryResponseDTO(
                        id,
                        "Ana",
                        "ana@test.com",
                        AccountKind.SALES_AGENT,
                        true,
                        null,
                        null,
                        Instant.parse("2026-01-01T00:00:00Z"),
                        true)));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ana"));
    }

    @Test
    void get_whenMissing_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(getUserByIdUseCase.execute(eq(id)))
                .thenThrow(new ResourceNotFoundException("usuário não encontrado: " + id));

        mockMvc.perform(get("/users/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
