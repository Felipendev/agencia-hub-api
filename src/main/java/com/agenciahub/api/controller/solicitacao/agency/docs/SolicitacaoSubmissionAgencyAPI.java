package com.agenciahub.api.controller.solicitacao.agency.docs;

import com.agenciahub.api.dto.solicitacao.SolicitacaoSubmissionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.UUID;

@RequestMapping("/agency/solicitacao-submissions")
@Tag(
        name = "Submissões do formulário público",
        description = "Listagem e exclusão de envios recebidos pelo formulário público da agência do tenant.")
public interface SolicitacaoSubmissionAgencyAPI {

    @GetMapping
    @Operation(summary = "Lista submissões", description = "Ordenação: mais recentes primeiro. Requer papel **OWNER**.")
    List<SolicitacaoSubmissionResponse> list();

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove submissão", description = "Após importar na cotação ou descartar o lead.")
    void delete(@Parameter(description = "id da submissão") @PathVariable UUID id);
}
