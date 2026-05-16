package com.agenciahub.api.application.usecases.auth.registerviainvite;

import com.agenciahub.api.application.usecases.invitation.shared.InvitationTokenPolicy;
import com.agenciahub.api.domain.InvitationStatus;
import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.domain.VerificationCodeType;
import com.agenciahub.api.application.usecases.auth.shared.RegisterAgencyResultDTO;
import com.agenciahub.api.application.usecases.auth.registerviainvite.RegisterViaInviteRequestDTO;
import com.agenciahub.api.application.persistence.entity.Invitation;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.application.persistence.repository.InvitationRepository;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.application.usecases.platformaccount.shared.PublicLinkCodeSupport;
import com.agenciahub.api.application.integrations.verification.VerificationCodePort;
import com.agenciahub.api.validation.PhoneValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RegisterViaInvite implements RegisterViaInviteUseCase {

    private final PlatformAccountRepository userRepository;
    private final InvitationRepository invitationRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationCodePort verificationCodePort;
    private final PublicLinkCodeSupport publicLinkCodeSupport;

    @Override
    @Transactional
    public RegisterAgencyResultDTO execute(RegisterViaInviteRequestDTO request) {
        Invitation invitation = invitationRepository
                .findWithAgencyByToken(request.token())
                .orElseThrow(() -> new ResourceNotFoundException("convite não encontrado: " + request.token()));
        InvitationTokenPolicy.ensureUsable(invitation);

        if (request.password().length() < 8) {
            throw new IllegalArgumentException("a senha deve ter no mínimo 8 caracteres");
        }

        if (!request.password().equals(request.passwordConfirmation())) {
            throw new IllegalArgumentException("as senhas não coincidem");
        }

        boolean validPhone = request.phone() != null && !request.phone().isBlank();
        if (validPhone) {
            if (!PhoneValidator.isValid(request.phone())) {
                throw new IllegalArgumentException("formato de telefone inválido. use DDD + número");
            }
        }

        String email = invitation.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("este e-mail já está cadastrado");
        }

        PlatformAccount user = PlatformAccount.builder()
                .agency(invitation.getAgency())
                .name(request.name())
                .email(email)
                .publicLinkCode(publicLinkCodeSupport.allocate())
                .passwordHash(passwordEncoder.encode(request.password()))
                .accountKind(AccountKind.SALES_AGENT)
                .phone(validPhone ? PhoneValidator.formatForStorage(request.phone()) : null)
                .emailVerified(false)
                .active(true)
                .build();
        user = userRepository.save(user);

        verificationCodePort.generateAndSend(
                email, VerificationCodeType.EMAIL_VERIFICATION, user, request.name());

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(Instant.now());
        invitationRepository.save(invitation);

        return new RegisterAgencyResultDTO(
                invitation.getAgency().getId(),
                user.getId(),
                "código de verificação enviado para " + email);
    }
}
