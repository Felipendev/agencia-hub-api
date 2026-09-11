package com.agenciahub.api.application.usecases.customer.shared;

import com.agenciahub.api.application.persistence.entity.CrmCustomer;

/**
 * Mapeamento entidade JPA → DTO de resposta (piloto ADR 0009 / doc 06).
 */
public final class OutputMapper {

    private OutputMapper() {
    }

    public static CustomerSummaryResponseDTO toSummary(CrmCustomer c) {
        return new CustomerSummaryResponseDTO(
                c.getId(),
                c.getName(),
                c.getEmail(),
                c.getPhone(),
                c.getInterestDestination(),
                c.getStatus(),
                c.getNotes(),
                c.getCreatedAt(),
                c.getProfileData());
    }
}
