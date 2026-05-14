package com.agenciahub.api.controller.auth;

import com.agenciahub.api.application.auth.ChangePasswordCommand;
import com.agenciahub.api.application.auth.ChangePasswordUseCase;
import com.agenciahub.api.application.auth.ForgotPasswordUseCase;
import com.agenciahub.api.application.auth.LoginUseCase;
import com.agenciahub.api.application.auth.RegisterAgencyUseCase;
import com.agenciahub.api.application.auth.RegisterViaInviteUseCase;
import com.agenciahub.api.application.auth.ResendCodeUseCase;
import com.agenciahub.api.application.auth.ResetPasswordUseCase;
import com.agenciahub.api.application.auth.ValidateInviteTokenUseCase;
import com.agenciahub.api.application.auth.VerifyEmailUseCase;
import com.agenciahub.api.controller.auth.docs.AuthAPI;
import com.agenciahub.api.dto.auth.ChangePasswordRequest;
import com.agenciahub.api.dto.auth.ForgotPasswordRequest;
import com.agenciahub.api.dto.auth.InviteValidationResponse;
import com.agenciahub.api.dto.auth.LoginRequest;
import com.agenciahub.api.dto.auth.LoginResponse;
import com.agenciahub.api.dto.auth.RegisterAgencyRequest;
import com.agenciahub.api.dto.auth.RegisterAgencyResponse;
import com.agenciahub.api.dto.auth.RegisterViaInviteRequest;
import com.agenciahub.api.dto.auth.ResendCodeRequest;
import com.agenciahub.api.dto.auth.ResetPasswordRequest;
import com.agenciahub.api.dto.auth.VerifyEmailRequest;
import com.agenciahub.api.dto.auth.VerifyEmailResponse;
import com.agenciahub.api.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthAPI {

    private final LoginUseCase loginUseCase;
    private final RegisterAgencyUseCase registerAgencyUseCase;
    private final VerifyEmailUseCase verifyEmailUseCase;
    private final ResendCodeUseCase resendCodeUseCase;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final ValidateInviteTokenUseCase validateInviteTokenUseCase;
    private final RegisterViaInviteUseCase registerViaInviteUseCase;

    @Override
    public LoginResponse login(LoginRequest request) {
        return loginUseCase.execute(request);
    }

    @Override
    public RegisterAgencyResponse register(RegisterAgencyRequest request) {
        return registerAgencyUseCase.execute(request);
    }

    @Override
    public VerifyEmailResponse verifyEmail(VerifyEmailRequest request) {
        return verifyEmailUseCase.execute(request);
    }

    @Override
    public Map<String, String> resendCode(ResendCodeRequest request) {
        return resendCodeUseCase.execute(request);
    }

    @Override
    public Map<String, String> forgotPassword(ForgotPasswordRequest request) {
        return forgotPasswordUseCase.execute(request);
    }

    @Override
    public Map<String, String> resetPassword(ResetPasswordRequest request) {
        return resetPasswordUseCase.execute(request);
    }

    @Override
    public Map<String, String> changePassword(ChangePasswordRequest request) {
        UUID currentUserId = getCurrentUserId();
        return changePasswordUseCase.execute(new ChangePasswordCommand(currentUserId, request));
    }

    @Override
    public InviteValidationResponse validateInvite(String token) {
        return validateInviteTokenUseCase.execute(token);
    }

    @Override
    public RegisterAgencyResponse registerViaInvite(RegisterViaInviteRequest request) {
        return registerViaInviteUseCase.execute(request);
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("usuário não autenticado");
        }
        if (authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return UUID.fromString(authentication.getName());
    }
}
