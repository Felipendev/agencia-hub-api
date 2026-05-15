package com.agenciahub.api.application.usecases.agency.shared;

import com.agenciahub.api.application.usecases.agency.shared.AgencySummaryResponseDTO;
import com.agenciahub.api.entity.Agency;
import org.springframework.stereotype.Component;

@Component
public class AgencyResponseMapper {

    public AgencySummaryResponseDTO toResponse(Agency agency) {
        return new AgencySummaryResponseDTO(
                agency.getId(),
                agency.getName(),
                agency.getPhone(),
                agency.getLogoUrl(),
                agency.getCnpj(),
                agency.getAddress(),
                agency.getAddressDetails(),
                agency.getCommercialEmail(),
                agency.getStatus(),
                agency.getSubscriptionStatus(),
                agency.getTrialEndsAt());
    }
}
