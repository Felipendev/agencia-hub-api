package com.agenciahub.api.application.controller.doc;

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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Map;

@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Cadastro, login, verificação de e-mail e fluxos de senha.")
@StandardErrorApiResponses
public interface AuthAPI {

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Autentica e retorna JWT com dados do usuário e da agência.")
    LoginResponseDTO login(@Valid @RequestBody LoginRequestDTO request);

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastro de agência", description = "Cria agência e usuário owner (regras de whitelist aplicadas).")
    RegisterAgencyResultDTO register(@Valid @RequestBody RegisterAgencyRequestDTO request);

    @PostMapping("/verify-email")
    @Operation(summary = "Confirmar e-mail", description = "Valida código de 6 dígitos enviado por e-mail.")
    VerifyEmailResponseDTO verifyEmail(@Valid @RequestBody VerifyEmailRequestDTO request);

    @PostMapping("/verify-email-link")
    @Operation(summary = "Confirmar e-mail via link", description = "Valida token opaco do link enviado por e-mail (auto-login).")
    VerifyEmailResponseDTO verifyEmailByLink(@Valid @RequestBody com.agenciahub.api.application.usecases.auth.verifyemailbylink.VerifyEmailByLinkRequestDTO request);

    @PostMapping("/resend-code")
    @Operation(summary = "Reenviar código", description = "Solicita novo código de verificação de e-mail.")
    Map<String, String> resendCode(@Valid @RequestBody ResendCodeRequestDTO request);

    @PostMapping("/forgot-password")
    @Operation(summary = "Esqueci a senha", description = "Solicita código para redefinição de senha.")
    Map<String, String> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request);

    @PostMapping("/reset-password")
    @Operation(summary = "Redefinir senha", description = "Define nova senha usando o código recebido.")
    Map<String, String> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request);

    @PostMapping("/change-password")
    @Operation(summary = "Alterar senha", description = "Usuário autenticado altera a senha atual.")
    Map<String, String> changePassword(@Valid @RequestBody ChangePasswordRequestDTO request);

    @GetMapping("/invite/{token}")
    @Operation(summary = "Validar convite", description = "Retorna dados do convite a partir do token público.")
    InviteValidationResponseDTO validateInvite(
            @Parameter(description = "token do convite") @PathVariable String token);

    @PostMapping("/register-invite")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastro via convite", description = "Registra vendedor a partir de token de convite.")
    RegisterAgencyResultDTO registerViaInvite(@Valid @RequestBody RegisterViaInviteRequestDTO request);

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Invalida o token JWT atual via last_logout_at.")
    Map<String, String> logout();

    @DeleteMapping("/account")
    @Operation(summary = "Solicitar exclusão de conta", description = "Agenda a exclusão da agência em 7 dias (grace period).")
    Map<String, String> requestAccountDeletion();
}
