package com.agenciahub.api.application.usecases.solicitacao.config.upsert;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitacaoConfigRequestDTO(
        @NotBlank @Size(min = 2, max = 64) String slug,
        @NotBlank @Size(max = 512) String tituloPagina,
        String textoIntro,
        String logoDataUrl,
        @Size(max = 255) String nomeMarca,
        JsonNode linksSociais
) {}
