package com.agenciahub.api.dto.agency;

import jakarta.validation.constraints.Size;

public record UpdateAgencyRequest(
        @Size(max = 255) String name,
        @Size(max = 32) String phone,
        @Size(max = 18) String cnpj,
        String address,
        @Size(max = 320) String commercialEmail
) {
}
