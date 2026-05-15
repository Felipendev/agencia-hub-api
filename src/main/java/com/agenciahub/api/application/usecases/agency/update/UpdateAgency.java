package com.agenciahub.api.application.usecases.agency.update;

import com.agenciahub.api.application.usecases.agency.shared.AgencyResponseMapper;
import com.agenciahub.api.application.usecases.agency.shared.AgencySummaryResponseDTO;
import com.agenciahub.api.application.usecases.agency.update.UpdateAgencyRequestDTO;
import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.entity.AgencyAuditLog;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.application.persistence.repository.AgencyAuditLogRepository;
import com.agenciahub.api.application.persistence.repository.AgencyRepository;
import com.agenciahub.api.validation.PhoneValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateAgency implements UpdateAgencyUseCase {

    private final AgencyRepository agencyRepository;
    private final AgencyAuditLogRepository auditLogRepository;
    private final AgencyResponseMapper agencyResponseMapper;

    @Override
    @Transactional
    public AgencySummaryResponseDTO execute(UpdateAgencyCommand command) {
        PlatformAccount user = command.currentUser();
        UUID id = user.getAgency().getId();
        Agency agency = agencyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("agência não encontrada: " + id));
        UpdateAgencyRequestDTO request = command.request();

        if (request.name() != null && !request.name().isBlank()) {
            auditField(agency, user, "name", agency.getName(), request.name());
            agency.setName(request.name());
        }

        if (request.phone() != null) {
            if (!request.phone().isBlank() && !PhoneValidator.isValid(request.phone())) {
                throw new IllegalArgumentException("formato de telefone inválido. use ddd + número");
            }
            auditField(agency, user, "phone", agency.getPhone(), request.phone());
            agency.setPhone(request.phone());
        }

        if (request.cnpj() != null) {
            validateCnpj(request.cnpj());
            auditField(agency, user, "cnpj", agency.getCnpj(), request.cnpj());
            agency.setCnpj(request.cnpj());
        }

        if (request.address() != null) {
            auditField(agency, user, "address", agency.getAddress(), request.address());
            agency.setAddress(request.address());
        }

        if (request.addressDetails() != null) {
            auditField(
                    agency,
                    user,
                    "address_details",
                    agency.getAddressDetails() == null ? null : agency.getAddressDetails().toString(),
                    request.addressDetails().toString());
            agency.setAddressDetails(request.addressDetails());
        }

        if (request.commercialEmail() != null) {
            auditField(agency, user, "commercial_email", agency.getCommercialEmail(), request.commercialEmail());
            agency.setCommercialEmail(request.commercialEmail());
        }

        if (request.logoUrl() != null) {
            if (!request.logoUrl().isEmpty() && !request.logoUrl().startsWith("data:image/")) {
                throw new IllegalArgumentException("logo deve ser uma imagem válida");
            }
            auditField(agency, user, "logo_url", agency.getLogoUrl(), "(logo updated)");
            agency.setLogoUrl(request.logoUrl().isEmpty() ? null : request.logoUrl());
        }

        agency = agencyRepository.save(agency);
        return agencyResponseMapper.toResponse(agency);
    }

    private void validateCnpj(String cnpj) {
        if (cnpj == null || cnpj.isBlank()) {
            return;
        }

        String digits = cnpj.replaceAll("\\D", "");

        if (digits.length() != 14) {
            throw new IllegalArgumentException("cnpj inválido");
        }

        if (digits.chars().distinct().count() == 1) {
            throw new IllegalArgumentException("cnpj inválido");
        }

        int[] weights1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * weights1[i];
        }
        int remainder = sum % 11;
        int checkDigit1 = remainder < 2 ? 0 : 11 - remainder;
        if (Character.getNumericValue(digits.charAt(12)) != checkDigit1) {
            throw new IllegalArgumentException("cnpj inválido");
        }

        int[] weights2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        sum = 0;
        for (int i = 0; i < 13; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * weights2[i];
        }
        remainder = sum % 11;
        int checkDigit2 = remainder < 2 ? 0 : 11 - remainder;
        if (Character.getNumericValue(digits.charAt(13)) != checkDigit2) {
            throw new IllegalArgumentException("cnpj inválido");
        }
    }

    private void auditField(Agency agency, PlatformAccount user, String fieldName, String oldValue, String newValue) {
        if (Objects.equals(oldValue, newValue)) {
            return;
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
