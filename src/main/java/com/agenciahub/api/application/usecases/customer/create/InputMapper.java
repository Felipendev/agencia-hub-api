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
        String email = blankToNull(request.email());
        String phone = blankToNull(request.phone());
        String notes = request.notes() != null ? request.notes() : "";
        String interestDestination = blankToNull(request.interestDestination());
        return CrmCustomer.builder()
                .name(request.name().strip())
                .email(email)
                .phone(phone)
                .interestDestination(interestDestination)
                .status(request.status())
                .profileData(request.profileData() == null ? null : request.profileData().deepCopy())
                .notes(notes)
                .build();
    }

    public static String normalizedPhone(CreateCustomerRequestDTO request) {
        return CustomerPhoneNormalizer.normalize(request.phone());
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
