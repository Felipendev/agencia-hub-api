package com.agenciahub.api.application.usecases.auth.login;

import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.application.usecases.auth.login.LoginRequestDTO;
import com.agenciahub.api.application.usecases.auth.login.LoginResponseDTO;
import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.exception.EmailNotVerifiedException;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.security.JwtService;
import com.agenciahub.api.application.usecases.platformaccount.shared.PublicLinkCodeSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Login implements LoginUseCase {

    private final PlatformAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PublicLinkCodeSupport publicLinkCodeSupport;

    @Override
    public LoginResponseDTO execute(LoginRequestDTO request) {
        PlatformAccount user = userRepository
                .findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("credenciais inválidas"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalStateException("usuário inativo.");
        }

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new EmailNotVerifiedException(user.getEmail());
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

        String publicLinkCode = publicLinkCodeSupport.ensurePersistedForUserId(user.getId());

        String token = jwtService.generate(
                user.getId(),
                user.getAccountKind().name(),
                agency != null ? agency.getId() : null,
                user.getPasswordChangedAt());

        return new LoginResponseDTO(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAccountKind(),
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
