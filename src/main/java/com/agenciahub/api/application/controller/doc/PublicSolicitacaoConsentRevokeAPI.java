package com.agenciahub.api.application.controller.doc;

import com.agenciahub.api.application.usecases.solicitacao.pub.consent.RevokeConsentRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/public/solicitacao/consent")
@Tag(
        name = "Solicitação pública",
        description = "Envio do formulário público de solicitação de orçamento (sem autenticação).")
@StandardErrorApiResponses
public interface PublicSolicitacaoConsentRevokeAPI {

    @PostMapping("/revoke")
    @Operation(
            summary = "Revoga consentimento LGPD",
            description = "Revoga o consentimento de todas as submissões do titular identificado por e-mail e telefone.")
    void revokeConsent(@Valid @RequestBody RevokeConsentRequestDTO body);
}
