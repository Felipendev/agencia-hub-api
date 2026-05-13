package com.agenciahub.api.service;

import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.domain.InvitationStatus;
import com.agenciahub.api.domain.SubscriptionStatus;
import com.agenciahub.api.domain.UserRole;
import com.agenciahub.api.domain.VerificationCodeType;
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
import com.agenciahub.api.entity.Agency;
import com.agenciahub.api.entity.Invitation;
import com.agenciahub.api.entity.TermsAcceptance;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.AgencyRepository;
import com.agenciahub.api.repository.InvitationRepository;
import com.agenciahub.api.repository.TermsAcceptanceRepository;
import com.agenciahub.api.repository.UserRepository;
import com.agenciahub.api.security.JwtService;
import com.agenciahub.api.validation.PhoneValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    // Whitelist: only allowed emails can register during beta
    private static final Set<String> ALLOWED_OWNER_EMAILS = Set.of(
        "contato@agenciashub.com.br",
        "consultoria.andressaviagens@gmail.com"
    );

    private final UserRepository userRepository;
    private final AgencyRepository agencyRepository;
    private final TermsAcceptanceRepository termsAcceptanceRepository;
    private final InvitationRepository invitationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final VerificationCodeService verificationCodeService;
    private final InvitationService invitationService;
    private final PublicLinkCodeService publicLinkCodeService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Credenciais inválidas"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalStateException("Usuário inativo.");
        }

        // Check email verification
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalStateException("Verifique seu e-mail para acessar o sistema");
        }

        // Check agency status
        Agency agency = user.getAgency();
        if (agency != null) {
            AgencyStatus agencyStatus = agency.getStatus();
            if (agencyStatus == AgencyStatus.PENDING_VERIFICATION || agencyStatus == AgencyStatus.CANCELED) {
                throw new IllegalStateException("Sua agência não está ativa. Entre em contato com o suporte.");
            }
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResourceNotFoundException("Credenciais inválidas");
        }

        String publicLinkCode = publicLinkCodeService.ensurePersistedForUserId(user.getId());

        String token = jwtService.generate(
                user.getId(),
                user.getRole().name(),
                agency != null ? agency.getId() : null,
                user.getPasswordChangedAt()
        );

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
                publicLinkCode
        );
    }

    // ─── Registration (Task 11.1) ────────────────────────────────────────────────

    @Transactional
    public RegisterAgencyResponse register(RegisterAgencyRequest request, String ipAddress) {
        String email = request.email().trim().toLowerCase();

        // Whitelist check: only allowed emails can register during beta
        if (!ALLOWED_OWNER_EMAILS.contains(email)) {
            throw new IllegalArgumentException("O sistema está em fase de testes. Cadastro restrito por convite.");
        }

        // Validate email uniqueness (case-insensitive)
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Este e-mail já está cadastrado");
        }

        // Validate password confirmation match
        if (!request.password().equals(request.passwordConfirmation())) {
            throw new IllegalArgumentException("As senhas não coincidem");
        }

        // Validate password length
        if (request.password().length() < 8) {
            throw new IllegalArgumentException("A senha deve ter no mínimo 8 caracteres");
        }

        // Validate phone format
        if (!PhoneValidator.isValid(request.ownerPhone())) {
            throw new IllegalArgumentException("Formato de telefone inválido. Use DDD + número");
        }

        // Validate terms acceptance
        if (!Boolean.TRUE.equals(request.termsAccepted())) {
            throw new IllegalArgumentException("É necessário aceitar os Termos de Uso");
        }

        // Validate optional agency phone
        if (request.agencyPhone() != null && !request.agencyPhone().isBlank()
                && !PhoneValidator.isValid(request.agencyPhone())) {
            throw new IllegalArgumentException("Formato de telefone da agência inválido. Use DDD + número");
        }

        // Create Agency
        Agency agency = Agency.builder()
                .name(request.agencyName())
                .phone(request.agencyPhone() != null && !request.agencyPhone().isBlank()
                        ? PhoneValidator.formatForStorage(request.agencyPhone()) : null)
                .status(AgencyStatus.PENDING_VERIFICATION)
                .subscriptionStatus(SubscriptionStatus.TRIAL)
                .build();
        agency = agencyRepository.save(agency);

        // Create Owner User
        User user = User.builder()
                .agency(agency)
                .name(request.ownerName())
                .email(email)
                .publicLinkCode(publicLinkCodeService.allocate())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.OWNER)
                .phone(PhoneValidator.formatForStorage(request.ownerPhone()))
                .emailVerified(false)
                .active(true)
                .build();
        user = userRepository.save(user);

        // Record terms acceptance
        TermsAcceptance termsAcceptance = TermsAcceptance.builder()
                .user(user)
                .termsVersion(request.termsVersion())
                .ipAddress(ipAddress)
                .build();
        termsAcceptanceRepository.save(termsAcceptance);

        verificationCodeService.generateAndSend(
                email,
                VerificationCodeType.EMAIL_VERIFICATION,
                user,
                request.ownerName()
        );

        return new RegisterAgencyResponse(
                agency.getId(),
                user.getId(),
                "Código de verificação enviado para " + email
        );
    }

    @Transactional
    public VerifyEmailResponse verifyEmail(VerifyEmailRequest request) {
        String email = request.email().trim().toLowerCase();

        // Validate code
        boolean valid = verificationCodeService.verify(email, request.code(), VerificationCodeType.EMAIL_VERIFICATION);
        if (!valid) {
            throw new IllegalArgumentException("Código inválido ou expirado");
        }

        // Find user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        // Activate user
        user.setEmailVerified(true);
        userRepository.save(user);

        // Transition agency to TRIAL
        Agency agency = user.getAgency();
        agency.setStatus(AgencyStatus.TRIAL);
        agency.setTrialEndsAt(Instant.now().plus(10, ChronoUnit.DAYS));
        agencyRepository.save(agency);

        // Generate JWT
        String publicLinkCode = publicLinkCodeService.ensurePersistedForUserId(user.getId());

        String token = jwtService.generate(
                user.getId(),
                user.getRole().name(),
                agency.getId(),
                user.getPasswordChangedAt()
        );

        return new VerifyEmailResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                agency.getId(),
                agency.getName(),
                publicLinkCode
        );
    }

    // ─── Resend Code (Task 11.3) ─────────────────────────────────────────────────

    @Transactional
    public Map<String, String> resendCode(ResendCodeRequest request) {
        String email = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        boolean sent = verificationCodeService.generateAndSend(
                email,
                VerificationCodeType.EMAIL_VERIFICATION,
                user,
                user.getName()
        );

        if (!sent) {
            throw new IllegalStateException("Limite de reenvios atingido. Tente novamente mais tarde.");
        }

        return Map.of("message", "Código reenviado para " + email);
    }

    // ─── Forgot Password (Task 13.1) ─────────────────────────────────────────────

    @Transactional
    public Map<String, String> forgotPassword(ForgotPasswordRequest request) {
        String email = request.email().trim().toLowerCase();

        // Always return same response to prevent email enumeration
        userRepository.findByEmail(email).ifPresent(user ->
                verificationCodeService.generateAndSend(
                        email,
                        VerificationCodeType.PASSWORD_RESET,
                        user,
                        user.getName()
                )
        );

        return Map.of("message", "Se o e-mail estiver cadastrado, você receberá um código");
    }

    // ─── Reset Password (Task 13.2) ──────────────────────────────────────────────

    @Transactional
    public Map<String, String> resetPassword(ResetPasswordRequest request) {
        String email = request.email().trim().toLowerCase();

        // Validate password length
        if (request.newPassword().length() < 8) {
            throw new IllegalArgumentException("A senha deve ter no mínimo 8 caracteres");
        }

        // Validate password confirmation
        if (!request.newPassword().equals(request.newPasswordConfirmation())) {
            throw new IllegalArgumentException("As senhas não coincidem");
        }

        // Verify code
        boolean valid = verificationCodeService.verify(email, request.code(), VerificationCodeType.PASSWORD_RESET);
        if (!valid) {
            throw new IllegalArgumentException("Código inválido ou expirado");
        }

        // Find user and update password
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);

        return Map.of("message", "Senha redefinida com sucesso");
    }

    // ─── Change Password (Task 13.3) ─────────────────────────────────────────────

    @Transactional
    public Map<String, String> changePassword(ChangePasswordRequest request, UUID currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        // Validate current password
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Senha atual incorreta");
        }

        // Validate new password length
        if (request.newPassword().length() < 8) {
            throw new IllegalArgumentException("A senha deve ter no mínimo 8 caracteres");
        }

        // Validate password confirmation
        if (!request.newPassword().equals(request.newPasswordConfirmation())) {
            throw new IllegalArgumentException("As senhas não coincidem");
        }

        // Update password and passwordChangedAt
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangedAt(Instant.now());
        user.setMustChangePassword(Boolean.FALSE);
        userRepository.save(user);

        return Map.of("message", "Senha alterada com sucesso");
    }

    // ─── Register via Invite (Task 15.2) ─────────────────────────────────────────

    @Transactional
    public RegisterAgencyResponse registerViaInvite(RegisterViaInviteRequest request) {
        // Validate token
        Invitation invitation = invitationService.validateToken(request.token());

        // Validate password length
        if (request.password().length() < 8) {
            throw new IllegalArgumentException("A senha deve ter no mínimo 8 caracteres");
        }

        // Validate password confirmation match
        if (!request.password().equals(request.passwordConfirmation())) {
            throw new IllegalArgumentException("As senhas não coincidem");
        }

        // Validate phone format (optional)
        if (request.phone() != null && !request.phone().isBlank()) {
            if (!PhoneValidator.isValid(request.phone())) {
                throw new IllegalArgumentException("Formato de telefone inválido. Use DDD + número");
            }
        }

        // Check email uniqueness
        String email = invitation.getEmail().trim().toLowerCase();

        // Seller registration is authorized by the invitation itself — no whitelist needed

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Este e-mail já está cadastrado");
        }

        // Create SELLER user linked to invitation's agency
        User user = User.builder()
                .agency(invitation.getAgency())
                .name(request.name())
                .email(email)
                .publicLinkCode(publicLinkCodeService.allocate())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.SELLER)
                .phone(request.phone() != null && !request.phone().isBlank()
                        ? PhoneValidator.formatForStorage(request.phone()) : null)
                .emailVerified(false)
                .active(true)
                .build();
        user = userRepository.save(user);

        // Send verification code
        verificationCodeService.generateAndSend(
                email,
                VerificationCodeType.EMAIL_VERIFICATION,
                user,
                request.name()
        );

        // Mark invitation as ACCEPTED
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(Instant.now());
        invitationRepository.save(invitation);

        return new RegisterAgencyResponse(
                invitation.getAgency().getId(),
                user.getId(),
                "Código de verificação enviado para " + email
        );
    }
}
