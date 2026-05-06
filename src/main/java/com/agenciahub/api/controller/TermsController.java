package com.agenciahub.api.controller;

import com.agenciahub.api.entity.TermsAcceptance;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.TermsAcceptanceRepository;
import com.agenciahub.api.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Terms")
public class TermsController {

    private static final String CURRENT_TERMS_VERSION = "1.0.0";

    private final TermsAcceptanceRepository termsAcceptanceRepository;
    private final UserRepository userRepository;

    @GetMapping("/public/terms/latest")
    @Operation(summary = "Get current terms version info (public, no auth)")
    public Map<String, String> getLatestTerms() {
        return Map.of(
                "version", CURRENT_TERMS_VERSION,
                "title", "Termos de Uso - AgenciaHub",
                "url", "/termos"
        );
    }

    @PostMapping("/terms/accept")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record terms acceptance (authenticated)")
    public Map<String, String> acceptTerms(
            @Valid @RequestBody AcceptTermsRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        String ipAddress = getClientIp(httpRequest);

        TermsAcceptance acceptance = TermsAcceptance.builder()
                .user(user)
                .termsVersion(request.termsVersion())
                .ipAddress(ipAddress)
                .build();
        termsAcceptanceRepository.save(acceptance);

        return Map.of("message", "Termos aceitos com sucesso");
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("Usuário não autenticado");
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

    public record AcceptTermsRequest(
            @NotBlank String termsVersion
    ) {
    }
}
