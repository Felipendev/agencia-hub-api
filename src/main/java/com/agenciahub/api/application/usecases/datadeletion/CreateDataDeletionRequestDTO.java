package com.agenciahub.api.application.usecases.datadeletion;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateDataDeletionRequestDTO(
        @NotBlank @Email String email,
        String telefone,
        String motivo
) {}
