package com.agenciahub.api.application.agency;

import com.agenciahub.api.dto.agency.AgencyResponse;
import com.agenciahub.api.entity.Agency;
import com.agenciahub.api.service.AgencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetAgency implements GetAgencyUseCase {

    private final AgencyService agencyService;
    private final AgencyResponseMapper agencyResponseMapper;

    @Override
    public AgencyResponse execute(UUID agencyId) {
        if (agencyId == null) {
            throw new IllegalStateException("nenhuma agência no contexto do tenant");
        }
        Agency agency = agencyService.getById(agencyId);
        return agencyResponseMapper.toResponse(agency);
    }
}
