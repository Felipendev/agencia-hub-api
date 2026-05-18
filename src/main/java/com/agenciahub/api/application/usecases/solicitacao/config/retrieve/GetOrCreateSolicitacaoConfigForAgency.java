package com.agenciahub.api.application.usecases.solicitacao.config.retrieve;

import com.agenciahub.api.application.usecases.solicitacao.shared.SolicitacaoConfigSupport;
import com.agenciahub.api.application.usecases.solicitacao.shared.SolicitacaoConfigSummaryResponseDTO;
import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.entity.SolicitacaoConfig;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.application.persistence.repository.AgencyRepository;
import com.agenciahub.api.application.persistence.repository.SolicitacaoConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetOrCreateSolicitacaoConfigForAgency implements GetOrCreateSolicitacaoConfigForAgencyUseCase {

    private final SolicitacaoConfigRepository repository;
    private final AgencyRepository agencyRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public SolicitacaoConfigSummaryResponseDTO execute(UUID agencyId) {
        if (agencyId == null) {
            throw new IllegalStateException("nenhuma agência no contexto do tenant");
        }

        SolicitacaoConfig config = repository.findFirstByAgency_Id(agencyId).orElseGet(() -> {
            Agency agency = agencyRepository
                    .findById(agencyId)
                    .orElseThrow(() -> new ResourceNotFoundException("agência não encontrada: " + agencyId));
            SolicitacaoConfig created = SolicitacaoConfigSupport.newDefaultForAgency(agency, objectMapper);
            return repository.save(created);
        });

        SolicitacaoConfigSummaryResponseDTO response = SolicitacaoConfigSupport.toResponse(config);
        Agency agency = config.getAgency();
        return SolicitacaoConfigSupport.withAgencyLogoFallback(response, agency);
    }
}
