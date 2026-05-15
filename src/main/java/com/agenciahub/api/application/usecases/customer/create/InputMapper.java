package com.agenciahub.api.application.usecases.customer.create;

import com.agenciahub.api.application.persistence.entity.CrmCustomer;
import com.agenciahub.api.application.usecases.customer.shared.CustomerPhoneNormalizer;

/**
 * Mapeamento DTO de entrada → entidade JPA (piloto ADR 0009 / doc 06).
 */
public final class InputMapper {

    private InputMapper() {
    }

    public static CrmCustomer toNewEntity(CreateCustomerRequestDTO request) {
        String email = request.email().strip();
        String notes = request.notes() != null ? request.notes() : "";
        return CrmCustomer.builder()
                .name(request.name().strip())
                .email(email)
                .phone(request.phone().strip())
                .interestDestination(request.interestDestination().strip())
                .status(request.status())
                .notes(notes)
                .build();
    }

    public static String normalizedPhone(CreateCustomerRequestDTO request) {
        return CustomerPhoneNormalizer.normalize(request.phone());
    }
}
