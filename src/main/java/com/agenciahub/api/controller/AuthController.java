package com.agenciahub.api.controller;

import com.agenciahub.api.dto.auth.ChangePasswordRequest;
import com.agenciahub.api.dto.auth.ForgotPasswordRequest;
import com.agenciahub.api.dto.auth.LoginRequest;
import com.agenciahub.api.dto.auth.LoginResponse;
import com.agenciahub.api.dto.auth.RegisterAgencyRequest;
import com.agenciahub.api.dto.auth.RegisterAgencyResponse;
import com.agenciahub.api.dto.auth.RegisterViaInviteRequest;
import com.agenciahub.api.dto.auth.ResendCodeRequest;
import com.agenciahub.api.dto.auth.ResetPasswordRequest;
import com.agenciahub.api.dto.auth.VerifyEmailRequest;
import com.agenciahub.api.dto.auth.VerifyEmailResponse;
import com.agenciahub.api.entity.Invitation;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.repository.UserRepository;
import com.agenciahub.api.service.AuthService;
import com.agenciahub.api.service.InvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final InvitationService invitationService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive a JWT token")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new agency and owner")
    public RegisterAgencyResponse register(
            @Valid @RequestBody RegisterAgencyRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        return authService.register(request, ipAddress);
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email with 6-digit code")
    public VerifyEmailResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return authService.verifyEmail(request);
    }

    @PostMapping("/resend-code")
    @Operation(summary = "Resend verification code")
    public Map<String, String> resendCode(@Valid @RequestBody ResendCodeRequest request) {
        return authService.resendCode(request);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset code")
    public Map<String, String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with verification code")
    public Map<String, String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password (authenticated)")
    public Map<String, String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        UUID currentUserId = getCurrentUserId();
        return authService.changePassword(request, currentUserId);
    }

    @GetMapping("/invite/{token}")
    @Operation(summary = "Validate invite token and return invitation details")
    public Map<String, String> validateInvite(@PathVariable String token) {
        Invitation invitation = invitationService.validateToken(token);
        return Map.of(
                "email", invitation.getEmail(),
                "agencyName", invitation.getAgency().getName()
        );
    }

    @PostMapping("/register-invite")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a seller via invitation token")
    public RegisterAgencyResponse registerViaInvite(@Valid @RequestBody RegisterViaInviteRequest request) {
        return authService.registerViaInvite(request);
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("Usuário não autenticado");
        }
        if (authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return UUID.fromString(authentication.getName());
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
