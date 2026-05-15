package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.auth.changepassword.ChangePasswordUseCase;
import com.agenciahub.api.application.usecases.auth.forgotpassword.ForgotPasswordUseCase;
import com.agenciahub.api.application.usecases.auth.login.LoginUseCase;
import com.agenciahub.api.application.usecases.auth.registeragency.RegisterAgencyUseCase;
import com.agenciahub.api.application.usecases.auth.registerviainvite.RegisterViaInviteUseCase;
import com.agenciahub.api.application.usecases.auth.resendcode.ResendCodeUseCase;
import com.agenciahub.api.application.usecases.auth.resetpassword.ResetPasswordUseCase;
import com.agenciahub.api.application.usecases.auth.validatetoken.ValidateInviteTokenUseCase;
import com.agenciahub.api.application.usecases.auth.verifyemail.VerifyEmailUseCase;
import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.domain.SubscriptionStatus;
import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.application.usecases.auth.login.LoginRequestDTO;
import com.agenciahub.api.application.usecases.auth.login.LoginResponseDTO;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcControllerTestImports.class)
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @MockitoBean
    private RegisterAgencyUseCase registerAgencyUseCase;

    @MockitoBean
    private VerifyEmailUseCase verifyEmailUseCase;

    @MockitoBean
    private ResendCodeUseCase resendCodeUseCase;

    @MockitoBean
    private ForgotPasswordUseCase forgotPasswordUseCase;

    @MockitoBean
    private ResetPasswordUseCase resetPasswordUseCase;

    @MockitoBean
    private ChangePasswordUseCase changePasswordUseCase;

    @MockitoBean
    private ValidateInviteTokenUseCase validateInviteTokenUseCase;

    @MockitoBean
    private RegisterViaInviteUseCase registerViaInviteUseCase;

    @Test
    void login_happyPath_returnsToken() throws Exception {
        UUID uid = UUID.randomUUID();
        UUID aid = UUID.randomUUID();
        when(loginUseCase.execute(any(LoginRequestDTO.class)))
                .thenReturn(new LoginResponseDTO(
                        "jwt-token",
                        uid,
                        "Nome",
                        "a@b.com",
                        AccountKind.AGENCY_OWNER,
                        aid,
                        "Agência",
                        AgencyStatus.ACTIVE,
                        SubscriptionStatus.ACTIVE,
                        Instant.parse("2030-01-01T00:00:00Z"),
                        false,
                        "code",
                        false));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"owner@test.com","password":"secret123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("a@b.com"));
    }

    @Test
    void login_invalidBody_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
