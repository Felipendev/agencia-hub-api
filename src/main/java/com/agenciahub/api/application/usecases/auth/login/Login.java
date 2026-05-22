package com.agenciahub.api.application.usecases.auth.login;

import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.exception.AccountDeletionPendingException;
import com.agenciahub.api.exception.EmailNotVerifiedException;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.security.JwtService;
import com.agenciahub.api.application.usecases.platformaccount.shared.PublicLinkCodeSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class Login implements LoginUseCase {

    private static final long MIN_RESPONSE_MS = 200;
    // Pre-computed BCrypt-10 hash used when the email does not exist, so the
    // password comparison always runs and takes the same time as a real lookup.
    private static final String DUMMY_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final PlatformAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PublicLinkCodeSupport publicLinkCodeSupport;

    @Override
    public LoginResponseDTO execute(LoginRequestDTO request) {
        long start = System.currentTimeMillis();
        try {
            Optional<PlatformAccount> maybeUser = userRepository
                    .findByEmail(request.email().trim().toLowerCase());

            // Always run BCrypt regardless of whether the email exists (SEC-05)
            String hash = maybeUser.map(PlatformAccount::getPasswordHash).orElse(DUMMY_HASH);
            boolean passwordMatches = passwordEncoder.matches(request.password(), hash);

            PlatformAccount user = maybeUser
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
                if (agencyStatus == AgencyStatus.DELETION_PENDING) {
                    throw new AccountDeletionPendingException();
                }
                if (agencyStatus == AgencyStatus.PENDING_VERIFICATION || agencyStatus == AgencyStatus.CANCELED) {
                    throw new IllegalStateException("sua agência não está ativa. entre em contato com o suporte.");
                }
            }

            if (!passwordMatches) {
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
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed < MIN_RESPONSE_MS) {
                try {
                    Thread.sleep(MIN_RESPONSE_MS - elapsed);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
