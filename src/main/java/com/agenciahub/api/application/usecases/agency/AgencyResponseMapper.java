package com.agenciahub.api.application.usecases.agency;

import com.agenciahub.api.dto.agency.AgencyResponse;
import com.agenciahub.api.entity.Agency;
import org.springframework.stereotype.Component;

@Component
public class AgencyResponseMapper {

    public AgencyResponse toResponse(Agency agency) {
        return new AgencyResponse(
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
