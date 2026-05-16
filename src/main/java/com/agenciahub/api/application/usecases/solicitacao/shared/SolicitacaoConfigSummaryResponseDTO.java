package com.agenciahub.api.application.usecases.solicitacao.shared;

import com.fasterxml.jackson.databind.JsonNode;

public record SolicitacaoConfigSummaryResponseDTO(
        String slug,
        String tituloPagina,
        String textoIntro,
        String logoDataUrl,
        String nomeMarca,
        JsonNode linksSociais
) {}
