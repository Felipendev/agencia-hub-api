package com.agenciahub.api.application.usecases.user.create;

import com.agenciahub.api.domain.enums.AccountKind;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateUserRequestDTO(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password,
        @NotNull AccountKind role,
        BigDecimal commissionPct,
        BigDecimal commissionFixed
) {
}
