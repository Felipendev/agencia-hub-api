package com.agenciahub.api.application.auth;

import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.dto.auth.LoginRequest;
import com.agenciahub.api.dto.auth.LoginResponse;
import com.agenciahub.api.entity.Agency;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.UserRepository;
import com.agenciahub.api.security.JwtService;
import com.agenciahub.api.service.PublicLinkCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Login implements LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PublicLinkCodeService publicLinkCodeService;

    @Override
    public LoginResponse execute(LoginRequest request) {
        User user = userRepository
                .findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("credenciais inválidas"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalStateException("usuário inativo.");
        }

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalStateException("verifique seu e-mail para acessar o sistema");
        }

        Agency agency = user.getAgency();
        if (agency != null) {
            AgencyStatus agencyStatus = agency.getStatus();
            if (agencyStatus == AgencyStatus.PENDING_VERIFICATION || agencyStatus == AgencyStatus.CANCELED) {
                throw new IllegalStateException("sua agência não está ativa. entre em contato com o suporte.");
            }
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResourceNotFoundException("credenciais inválidas");
        }

        String publicLinkCode = publicLinkCodeService.ensurePersistedForUserId(user.getId());

        String token = jwtService.generate(
                user.getId(),
                user.getRole().name(),
                agency != null ? agency.getId() : null,
                user.getPasswordChangedAt());

        return new LoginResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                agency != null ? agency.getId() : null,
                agency != null ? agency.getName() : null,
                agency != null ? agency.getStatus() : null,
                agency != null ? agency.getSubscriptionStatus() : null,
                agency != null ? agency.getTrialEndsAt() : null,
                Boolean.TRUE.equals(user.getMustChangePassword()) ? Boolean.TRUE : null,
                publicLinkCode,
                !Boolean.TRUE.equals(user.getTermsAccepted()));
    }
}
