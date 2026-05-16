package com.agenciahub.api.application.controller.doc;

import com.agenciahub.api.application.usecases.solicitacao.pub.submit.PublicSolicitacaoSubmitRequestDTO;
import com.agenciahub.api.application.usecases.solicitacao.pub.submit.PublicSolicitacaoSubmitResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@RequestMapping("/public/solicitacao")
@Tag(
        name = "Solicitação pública",
        description = "Envio do formulário público de solicitação de orçamento (sem autenticação).")
@StandardErrorApiResponses
public interface PublicSolicitacaoSubmitAPI {

    @PostMapping("/submit")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Registra envio do formulário",
            description = "Persiste a submissão pública; valida telefone e presença de origem/destino nos detalhes.")
    PublicSolicitacaoSubmitResponseDTO submit(@Valid @RequestBody PublicSolicitacaoSubmitRequestDTO body);
}
