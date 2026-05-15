package com.agenciahub.api.application.usecases.agency.update;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Size;

public record UpdateAgencyRequestDTO(
        @Size(max = 255) String name,
        @Size(max = 32) String phone,
        @Size(max = 18) String cnpj,
        String address,
        JsonNode addressDetails,
        @Size(max = 320) String commercialEmail,
        String logoUrl
) {
}
