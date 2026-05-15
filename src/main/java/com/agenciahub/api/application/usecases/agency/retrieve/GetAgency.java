package com.agenciahub.api.application.usecases.agency.retrieve;

import com.agenciahub.api.application.usecases.agency.shared.AgencyResponseMapper;
import com.agenciahub.api.application.usecases.agency.shared.AgencySummaryResponseDTO;
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
    public AgencySummaryResponseDTO execute(UUID agencyId) {
        if (agencyId == null) {
            throw new IllegalStateException("nenhuma agência no contexto do tenant");
        }
        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("agência não encontrada: " + agencyId));
        return agencyResponseMapper.toResponse(agency);
    }
}
