package com.agenciahub.api.application.usecases.auth.registeragency;

import com.agenciahub.api.application.usecases.auth.shared.AuthBetaWhitelist;
import com.agenciahub.api.application.usecases.terms.shared.TermsConstants;
import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.domain.SubscriptionStatus;
import com.agenciahub.api.domain.UserRole;
import com.agenciahub.api.domain.VerificationCodeType;
import com.agenciahub.api.application.usecases.auth.registeragency.RegisterAgencyRequestDTO;
import com.agenciahub.api.application.usecases.auth.shared.RegisterAgencyResultDTO;
import com.agenciahub.api.entity.Agency;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.repository.AgencyRepository;
import com.agenciahub.api.repository.UserRepository;
import com.agenciahub.api.application.usecases.user.shared.PublicLinkCodeSupport;
import com.agenciahub.api.application.integrations.verification.VerificationCodePort;
import com.agenciahub.api.validation.PhoneValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterAgency implements RegisterAgencyUseCase {

    private final UserRepository userRepository;
    private final AgencyRepository agencyRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationCodePort verificationCodePort;
    private final PublicLinkCodeSupport publicLinkCodeSupport;

    @Override
    @Transactional
    public RegisterAgencyResultDTO execute(RegisterAgencyRequestDTO request) {
        String email = request.email().trim().toLowerCase();

        if (!AuthBetaWhitelist.ALLOWED_OWNER_EMAILS.contains(email)) {
            throw new IllegalArgumentException("o sistema está em fase de testes. cadastro restrito por convite.");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("este e-mail já está cadastrado");
        }

        if (!request.password().equals(request.passwordConfirmation())) {
            throw new IllegalArgumentException("as senhas não coincidem");
        }

        if (request.password().length() < 8) {
            throw new IllegalArgumentException("a senha deve ter no mínimo 8 caracteres");
        }

        if (!PhoneValidator.isValid(request.ownerPhone())) {
            throw new IllegalArgumentException("formato de telefone inválido. use DDD + número");
        }

        if (!Boolean.TRUE.equals(request.termsAccepted())) {
            throw new IllegalArgumentException("é necessário aceitar os termos de uso");
        }
        if (!TermsConstants.CURRENT_TERMS_VERSION.equals(request.termsVersion())) {
            throw new IllegalArgumentException("versão dos termos inválida");
        }

        if (request.agencyPhone() != null
                && !request.agencyPhone().isBlank()
                && !PhoneValidator.isValid(request.agencyPhone())) {
            throw new IllegalArgumentException("formato de telefone da agência inválido. use DDD + número");
        }

        Agency agency = Agency.builder()
                .name(request.agencyName())
                .phone(request.agencyPhone() != null && !request.agencyPhone().isBlank()
                        ? PhoneValidator.formatForStorage(request.agencyPhone())
                        : null)
                .status(AgencyStatus.PENDING_VERIFICATION)
                .subscriptionStatus(SubscriptionStatus.TRIAL)
                .build();
        agency = agencyRepository.save(agency);

        User user = User.builder()
                .agency(agency)
                .name(request.ownerName())
                .email(email)
                .publicLinkCode(publicLinkCodeSupport.allocate())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.OWNER)
                .phone(PhoneValidator.formatForStorage(request.ownerPhone()))
                .emailVerified(false)
                .active(true)
                .termsAccepted(true)
                .build();
        user = userRepository.save(user);

        verificationCodePort.generateAndSend(
                email, VerificationCodeType.EMAIL_VERIFICATION, user, request.ownerName());

        return new RegisterAgencyResultDTO(
                agency.getId(), user.getId(), "código de verificação enviado para " + email);
    }
}
