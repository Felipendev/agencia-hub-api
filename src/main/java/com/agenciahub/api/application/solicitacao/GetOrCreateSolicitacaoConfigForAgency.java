package com.agenciahub.api.application.solicitacao;

import com.agenciahub.api.dto.solicitacao.SolicitacaoConfigResponse;
import com.agenciahub.api.entity.Agency;
import com.agenciahub.api.entity.SolicitacaoConfig;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.AgencyRepository;
import com.agenciahub.api.repository.SolicitacaoConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetOrCreateSolicitacaoConfigForAgency implements GetOrCreateSolicitacaoConfigForAgencyUseCase {

    private final SolicitacaoConfigRepository repository;
    private final AgencyRepository agencyRepository;
    private final ObjectMapper objectMapper;

    @Override
    public SolicitacaoConfigResponse execute(UUID agencyId) {
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

        SolicitacaoConfigResponse response = SolicitacaoConfigSupport.toResponse(config);
        Agency agency = config.getAgency();
        return SolicitacaoConfigSupport.withAgencyLogoFallback(response, agency);
    }
}
