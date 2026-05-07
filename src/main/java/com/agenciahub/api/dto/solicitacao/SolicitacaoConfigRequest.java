package com.agenciahub.api.dto.solicitacao;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitacaoConfigRequest(
        @NotBlank @Size(min = 2, max = 64) String slug,
        @NotBlank @Size(max = 512) String tituloPagina,
        String textoIntro,
        String logoDataUrl,
        @Size(max = 255) String nomeMarca,
        JsonNode linksSociais
) {}
