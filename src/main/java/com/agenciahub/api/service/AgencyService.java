package com.agenciahub.api.service;

import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.domain.SubscriptionStatus;
import com.agenciahub.api.dto.agency.UpdateAgencyRequest;
import com.agenciahub.api.entity.Agency;
import com.agenciahub.api.entity.AgencyAuditLog;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.AgencyAuditLogRepository;
import com.agenciahub.api.repository.AgencyRepository;
import com.agenciahub.api.repository.UserRepository;
import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgencyService {

    private final AgencyRepository agencyRepository;
    private final AgencyAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    /**
     * Creates a new agency with the given parameters.
     */
    @Transactional
    public Agency create(String name, String phone, AgencyStatus status, SubscriptionStatus subStatus) {
        Agency agency = Agency.builder()
                .name(name)
                .phone(phone)
                .status(status)
                .subscriptionStatus(subStatus)
                .build();
        return agencyRepository.save(agency);
    }

    /**
     * Returns an agency by its ID.
     */
    public Agency getById(UUID id) {
        return agencyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agência não encontrada"));
    }

    /**
     * Updates agency fields and records audit log entries for each change.
     */
    @Transactional
    public Agency update(UUID id, UpdateAgencyRequest request, User currentUser) {
        Agency agency = getById(id);

        if (request.name() != null && !request.name().isBlank()) {
            auditField(agency, currentUser, "name", agency.getName(), request.name());
            agency.setName(request.name());
        }

        if (request.phone() != null) {
            auditField(agency, currentUser, "phone", agency.getPhone(), request.phone());
            agency.setPhone(request.phone());
        }

        if (request.cnpj() != null) {
            validateCnpj(request.cnpj());
            auditField(agency, currentUser, "cnpj", agency.getCnpj(), request.cnpj());
            agency.setCnpj(request.cnpj());
        }

        if (request.address() != null) {
            auditField(agency, currentUser, "address", agency.getAddress(), request.address());
            agency.setAddress(request.address());
        }

        if (request.commercialEmail() != null) {
            auditField(agency, currentUser, "commercial_email", agency.getCommercialEmail(), request.commercialEmail());
            agency.setCommercialEmail(request.commercialEmail());
        }

        return agencyRepository.save(agency);
    }

    /**
     * Returns the current user's agency based on TenantContext.
     */
    public Agency getCurrentAgency() {
        UUID agencyId = TenantContext.get();
        if (agencyId == null) {
            throw new IllegalStateException("Nenhuma agência no contexto do tenant");
        }
        return getById(agencyId);
    }

    /**
     * Updates the agency logo URL and records an audit log entry.
     */
    @Transactional
    public Agency updateLogo(UUID agencyId, String logoUrl, User currentUser) {
        Agency agency = getById(agencyId);
        auditField(agency, currentUser, "logo_url", agency.getLogoUrl(), logoUrl);
        agency.setLogoUrl(logoUrl);
        return agencyRepository.save(agency);
    }

    /**
     * Validates CNPJ format: 14 digits with valid check digits.
     * Accepts raw digits (14 chars) or formatted (XX.XXX.XXX/XXXX-XX).
     */
    public void validateCnpj(String cnpj) {
        if (cnpj == null || cnpj.isBlank()) {
            return; // null/blank is allowed (optional field)
        }

        String digits = cnpj.replaceAll("\\D", "");

        if (digits.length() != 14) {
            throw new IllegalArgumentException("CNPJ inválido");
        }

        // Reject all-same-digit CNPJs
        if (digits.chars().distinct().count() == 1) {
            throw new IllegalArgumentException("CNPJ inválido");
        }

        // Validate first check digit
        int[] weights1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * weights1[i];
        }
        int remainder = sum % 11;
        int checkDigit1 = remainder < 2 ? 0 : 11 - remainder;
        if (Character.getNumericValue(digits.charAt(12)) != checkDigit1) {
            throw new IllegalArgumentException("CNPJ inválido");
        }

        // Validate second check digit
        int[] weights2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        sum = 0;
        for (int i = 0; i < 13; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * weights2[i];
        }
        remainder = sum % 11;
        int checkDigit2 = remainder < 2 ? 0 : 11 - remainder;
        if (Character.getNumericValue(digits.charAt(13)) != checkDigit2) {
            throw new IllegalArgumentException("CNPJ inválido");
        }
    }

    /**
     * Records an audit log entry for a field change.
     */
    private void auditField(Agency agency, User user, String fieldName, String oldValue, String newValue) {
        if (Objects.equals(oldValue, newValue)) {
            return; // No change, no audit
        }

        AgencyAuditLog log = AgencyAuditLog.builder()
                .agency(agency)
                .user(user)
                .action("UPDATE")
                .fieldName(fieldName)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();
        auditLogRepository.save(log);
    }
}
