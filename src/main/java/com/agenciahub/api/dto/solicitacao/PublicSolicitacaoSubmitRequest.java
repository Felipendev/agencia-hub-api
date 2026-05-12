package com.agenciahub.api.dto.solicitacao;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Body público do formulário de solicitação de orçamento (espelha o payload do Next.js).
 */
public record PublicSolicitacaoSubmitRequest(
        @NotBlank @Size(max = 128) String slug,
        @NotBlank @Size(max = 255) String nome,
        @Size(max = 320) String email,
        @NotBlank @Size(max = 32) String telefone,
        @NotNull JsonNode detalhes,
        @Size(max = 20000) String observacoes,
        /** Opcional: vendedor/dono na mesma agência — URL pública {@code ?vendedor=<uuid>}. */
        UUID referralSellerId
) {
}
