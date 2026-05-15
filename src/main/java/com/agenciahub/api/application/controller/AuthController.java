package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.auth.changepassword.ChangePasswordCommand;
import com.agenciahub.api.application.usecases.auth.changepassword.ChangePasswordUseCase;
import com.agenciahub.api.application.usecases.auth.forgotpassword.ForgotPasswordUseCase;
import com.agenciahub.api.application.usecases.auth.login.LoginUseCase;
import com.agenciahub.api.application.usecases.auth.registeragency.RegisterAgencyUseCase;
import com.agenciahub.api.application.usecases.auth.registerviainvite.RegisterViaInviteUseCase;
import com.agenciahub.api.application.usecases.auth.resendcode.ResendCodeUseCase;
import com.agenciahub.api.application.usecases.auth.resetpassword.ResetPasswordUseCase;
import com.agenciahub.api.application.usecases.auth.validatetoken.ValidateInviteTokenUseCase;
import com.agenciahub.api.application.usecases.auth.verifyemail.VerifyEmailUseCase;
import com.agenciahub.api.application.controller.doc.AuthAPI;
import com.agenciahub.api.application.usecases.auth.changepassword.ChangePasswordRequestDTO;
import com.agenciahub.api.application.usecases.auth.forgotpassword.ForgotPasswordRequestDTO;
import com.agenciahub.api.application.usecases.auth.validatetoken.InviteValidationResponseDTO;
import com.agenciahub.api.application.usecases.auth.login.LoginRequestDTO;
import com.agenciahub.api.application.usecases.auth.login.LoginResponseDTO;
import com.agenciahub.api.application.usecases.auth.registeragency.RegisterAgencyRequestDTO;
import com.agenciahub.api.application.usecases.auth.shared.RegisterAgencyResultDTO;
import com.agenciahub.api.application.usecases.auth.registerviainvite.RegisterViaInviteRequestDTO;
import com.agenciahub.api.application.usecases.auth.resendcode.ResendCodeRequestDTO;
import com.agenciahub.api.application.usecases.auth.resetpassword.ResetPasswordRequestDTO;
import com.agenciahub.api.application.usecases.auth.verifyemail.VerifyEmailRequestDTO;
import com.agenciahub.api.application.usecases.auth.verifyemail.VerifyEmailResponseDTO;
import com.agenciahub.api.security.SecurityContextUsers;
import lombok.RequiredArgsConstructor;
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
    public LoginResponseDTO login(LoginRequestDTO request) {
        return loginUseCase.execute(request);
    }

    @Override
    public RegisterAgencyResultDTO register(RegisterAgencyRequestDTO request) {
        return registerAgencyUseCase.execute(request);
    }

    @Override
    public VerifyEmailResponseDTO verifyEmail(VerifyEmailRequestDTO request) {
        return verifyEmailUseCase.execute(request);
    }

    @Override
    public Map<String, String> resendCode(ResendCodeRequestDTO request) {
        return resendCodeUseCase.execute(request);
    }

    @Override
    public Map<String, String> forgotPassword(ForgotPasswordRequestDTO request) {
        return forgotPasswordUseCase.execute(request);
    }

    @Override
    public Map<String, String> resetPassword(ResetPasswordRequestDTO request) {
        return resetPasswordUseCase.execute(request);
    }

    @Override
    public Map<String, String> changePassword(ChangePasswordRequestDTO request) {
        UUID currentUserId = SecurityContextUsers.requireUserId();
        return changePasswordUseCase.execute(new ChangePasswordCommand(currentUserId, request));
    }

    @Override
    public InviteValidationResponseDTO validateInvite(String token) {
        return validateInviteTokenUseCase.execute(token);
    }

    @Override
    public RegisterAgencyResultDTO registerViaInvite(RegisterViaInviteRequestDTO request) {
        return registerViaInviteUseCase.execute(request);
    }
}
