package com.agenciahub.api.controller.solicitacao.pub.docs;

import com.agenciahub.api.api.docs.StandardErrorApiResponses;
import com.agenciahub.api.dto.solicitacao.SolicitacaoConfigResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/public/solicitacao-config")
@Tag(
        name = "Configuração pública do formulário",
        description = "Leitura da configuração do formulário de solicitação por slug (sem autenticação).")
@StandardErrorApiResponses
public interface PublicSolicitacaoConfigAPI {

    @GetMapping("/{slug}")
    @Operation(summary = "Obtém configuração por slug", description = "Usado pela página pública do formulário.")
    SolicitacaoConfigResponse getBySlug(@Parameter(description = "slug do formulário") @PathVariable String slug);
}
