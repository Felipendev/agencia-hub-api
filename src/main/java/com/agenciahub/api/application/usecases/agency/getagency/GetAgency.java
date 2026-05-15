package com.agenciahub.api.application.usecases.agency.getagency;

import com.agenciahub.api.application.usecases.agency.AgencyResponseMapper;
import com.agenciahub.api.dto.agency.AgencyResponse;
import com.agenciahub.api.entity.Agency;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.AgencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetAgency implements GetAgencyUseCase {

    private final AgencyRepository agencyRepository;
    private final AgencyResponseMapper agencyResponseMapper;

    @Override
    public AgencyResponse execute(UUID agencyId) {
        if (agencyId == null) {
            throw new IllegalStateException("nenhuma agência no contexto do tenant");
        }
        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("agência não encontrada: " + agencyId));
        return agencyResponseMapper.toResponse(agency);
    }
}
