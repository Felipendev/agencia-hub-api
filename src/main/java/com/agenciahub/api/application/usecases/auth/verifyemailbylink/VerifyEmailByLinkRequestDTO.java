package com.agenciahub.api.application.usecases.auth.verifyemailbylink;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailByLinkRequestDTO(@NotBlank String token) {}
