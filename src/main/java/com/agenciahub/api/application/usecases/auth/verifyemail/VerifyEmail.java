package com.agenciahub.api.application.usecases.auth.verifyemail;

import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.domain.VerificationCodeType;
import com.agenciahub.api.dto.auth.VerifyEmailRequest;
import com.agenciahub.api.dto.auth.VerifyEmailResponse;
import com.agenciahub.api.entity.Agency;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.AgencyRepository;
import com.agenciahub.api.repository.UserRepository;
import com.agenciahub.api.security.JwtService;
import com.agenciahub.api.application.usecases.user.PublicLinkCodeSupport;
import com.agenciahub.api.application.integrations.verification.VerificationCodePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class VerifyEmail implements VerifyEmailUseCase {

    private final UserRepository userRepository;
    private final AgencyRepository agencyRepository;
    private final VerificationCodePort verificationCodePort;
    private final PublicLinkCodeSupport publicLinkCodeSupport;
    private final JwtService jwtService;

    @Override
    @Transactional
    public VerifyEmailResponse execute(VerifyEmailRequest request) {
        String email = request.email().trim().toLowerCase();

        boolean valid =
                verificationCodePort.verify(email, request.code(), VerificationCodeType.EMAIL_VERIFICATION);
        if (!valid) {
            throw new IllegalArgumentException("código inválido ou expirado");
        }

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("usuário não encontrado: " + email));

        user.setEmailVerified(true);
        userRepository.save(user);

        Agency agency = user.getAgency();
        agency.setStatus(AgencyStatus.TRIAL);
        agency.setTrialEndsAt(Instant.now().plus(10, ChronoUnit.DAYS));
        agencyRepository.save(agency);

        String publicLinkCode = publicLinkCodeSupport.ensurePersistedForUserId(user.getId());

        String token = jwtService.generate(
                user.getId(),
                user.getRole().name(),
                agency.getId(),
                user.getPasswordChangedAt());

        return new VerifyEmailResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                agency.getId(),
                agency.getName(),
                publicLinkCode);
    }
}
