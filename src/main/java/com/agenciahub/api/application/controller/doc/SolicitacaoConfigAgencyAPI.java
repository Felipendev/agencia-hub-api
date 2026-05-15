package com.agenciahub.api.application.controller.doc;

import com.agenciahub.api.application.usecases.solicitacao.config.upsert.SolicitacaoConfigRequestDTO;
import com.agenciahub.api.application.usecases.solicitacao.shared.SolicitacaoConfigSummaryResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/agency/solicitacao-config")
@Tag(
        name = "Configuração do formulário (agência)",
        description = "CRUD da configuração do formulário público de solicitação para a agência do tenant. Acesso **OWNER**.")
@StandardErrorApiResponses
public interface SolicitacaoConfigAgencyAPI {

    @GetMapping
    @Operation(
            summary = "Obtém ou cria configuração",
            description = "Retorna a configuração da agência; se não existir, cria padrão e persiste.")
    SolicitacaoConfigSummaryResponseDTO get();

    @PutMapping
    @Operation(summary = "Atualiza configuração", description = "Upsert completo dos campos editáveis (inclui slug).")
    SolicitacaoConfigSummaryResponseDTO upsert(@Valid @RequestBody SolicitacaoConfigRequestDTO request);
}
