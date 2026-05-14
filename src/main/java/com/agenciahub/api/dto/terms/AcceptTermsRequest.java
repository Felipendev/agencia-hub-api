package com.agenciahub.api.dto.terms;

import jakarta.validation.constraints.NotBlank;

public record AcceptTermsRequest(@NotBlank String termsVersion) {}
