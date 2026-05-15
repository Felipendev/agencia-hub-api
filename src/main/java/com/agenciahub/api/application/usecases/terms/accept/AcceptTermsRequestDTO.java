package com.agenciahub.api.application.usecases.terms.accept;

import jakarta.validation.constraints.NotBlank;

public record AcceptTermsRequestDTO(@NotBlank String termsVersion) {}
