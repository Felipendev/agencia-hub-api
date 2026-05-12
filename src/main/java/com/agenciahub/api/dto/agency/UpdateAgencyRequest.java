package com.agenciahub.api.dto.agency;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Size;

public record UpdateAgencyRequest(
        @Size(max = 255) String name,
        @Size(max = 32) String phone,
        @Size(max = 18) String cnpj,
        String address,
        JsonNode addressDetails,
        @Size(max = 320) String commercialEmail,
        String logoUrl
) {
}
