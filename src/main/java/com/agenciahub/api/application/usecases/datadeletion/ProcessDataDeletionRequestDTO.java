package com.agenciahub.api.application.usecases.datadeletion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ProcessDataDeletionRequestDTO(
        @NotBlank @Pattern(regexp = "EXCLUIR|REJEITAR") String acao,
        String justificativa
) {}
