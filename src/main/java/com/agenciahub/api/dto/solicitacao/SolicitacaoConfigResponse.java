package com.agenciahub.api.dto.solicitacao;

import com.fasterxml.jackson.databind.JsonNode;

public record SolicitacaoConfigResponse(
        String slug,
        String tituloPagina,
        String textoIntro,
        String logoDataUrl,
        String nomeMarca,
        JsonNode linksSociais
) {}
