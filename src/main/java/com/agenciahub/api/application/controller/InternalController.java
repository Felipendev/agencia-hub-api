package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.exception.ApiError;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private final PlatformAccountRepository platformAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.internal-api-key:}")
    private String internalApiKey;

    public record SetupAdminRequestDTO(
            @NotBlank String name,
            @NotBlank String email,
            @NotBlank String password
    ) {}

    @PostMapping("/setup-admin")
    public ResponseEntity<?> setupAdmin(
            @RequestHeader("X-Internal-Key") String key,
            @Valid @RequestBody SetupAdminRequestDTO request) {

        if (internalApiKey == null || internalApiKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiError("internal API key not configured", "FORBIDDEN"));
        }

        if (!internalApiKey.equals(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiError("invalid internal key", "FORBIDDEN"));
        }

        boolean adminExists = platformAccountRepository.findAllByOrderByNameAsc().stream()
                .anyMatch(u -> u.getAccountKind() == AccountKind.PLATFORM_ADMIN);

        if (adminExists) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError("platform admin already exists", "CONFLICT"));
        }

        PlatformAccount admin = PlatformAccount.builder()
                .name(request.name())
                .email(request.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .accountKind(AccountKind.PLATFORM_ADMIN)
                .emailVerified(true)
                .active(true)
                .mustChangePassword(true)
                .termsAccepted(true)
                .build();

        platformAccountRepository.save(admin);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(java.util.Map.of("message", "platform admin created successfully"));
    }
}
