package com.agenciahub.api.application.controllers.docs;

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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
    LoginResponse login(@Valid @RequestBody LoginRequest request);

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastro de agência", description = "Cria agência e usuário owner (regras de whitelist aplicadas).")
    RegisterAgencyResponse register(@Valid @RequestBody RegisterAgencyRequest request);

    @PostMapping("/verify-email")
    @Operation(summary = "Confirmar e-mail", description = "Valida código de 6 dígitos enviado por e-mail.")
    VerifyEmailResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request);

    @PostMapping("/resend-code")
    @Operation(summary = "Reenviar código", description = "Solicita novo código de verificação de e-mail.")
    Map<String, String> resendCode(@Valid @RequestBody ResendCodeRequest request);

    @PostMapping("/forgot-password")
    @Operation(summary = "Esqueci a senha", description = "Solicita código para redefinição de senha.")
    Map<String, String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request);

    @PostMapping("/reset-password")
    @Operation(summary = "Redefinir senha", description = "Define nova senha usando o código recebido.")
    Map<String, String> resetPassword(@Valid @RequestBody ResetPasswordRequest request);

    @PostMapping("/change-password")
    @Operation(summary = "Alterar senha", description = "Usuário autenticado altera a senha atual.")
    Map<String, String> changePassword(@Valid @RequestBody ChangePasswordRequest request);

    @GetMapping("/invite/{token}")
    @Operation(summary = "Validar convite", description = "Retorna dados do convite a partir do token público.")
    InviteValidationResponse validateInvite(
            @Parameter(description = "token do convite") @PathVariable String token);

    @PostMapping("/register-invite")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastro via convite", description = "Registra vendedor a partir de token de convite.")
    RegisterAgencyResponse registerViaInvite(@Valid @RequestBody RegisterViaInviteRequest request);
}
