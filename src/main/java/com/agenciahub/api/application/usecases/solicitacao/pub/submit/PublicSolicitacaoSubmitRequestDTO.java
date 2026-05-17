package com.agenciahub.api.application.usecases.solicitacao.pub.submit;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Body público do formulário de solicitação de orçamento (espelha o payload do Next.js).
 *
 * <p>Indicação de vendedor: use {@code sellerPublicCode} (query {@code ?vendedor=}) quando possível;
 * {@code referralSellerId} permanece para compatibilidade. Se ambos vierem preenchidos, prevalece o código.
 */
public record PublicSolicitacaoSubmitRequestDTO(
        @NotBlank @Size(max = 128) String slug,
        @NotBlank @Size(max = 255) String nome,
        @Size(max = 320) String email,
        @NotBlank @Size(max = 32) String telefone,
        @NotNull JsonNode detalhes,
        @Size(max = 20000) String observacoes,
        UUID referralSellerId,
        @Size(max = 16, message = "deve ter no máximo 16 caracteres") String sellerPublicCode
) {
}