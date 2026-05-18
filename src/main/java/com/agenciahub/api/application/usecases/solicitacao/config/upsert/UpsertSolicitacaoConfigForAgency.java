package com.agenciahub.api.application.usecases.solicitacao.config.upsert;

import com.agenciahub.api.application.usecases.solicitacao.shared.SolicitacaoConfigSupport;
import com.agenciahub.api.application.usecases.solicitacao.config.upsert.SolicitacaoConfigRequestDTO;
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
public class UpsertSolicitacaoConfigForAgency implements UpsertSolicitacaoConfigForAgencyUseCase {

    private final SolicitacaoConfigRepository repository;
    private final AgencyRepository agencyRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public SolicitacaoConfigSummaryResponseDTO execute(UpsertSolicitacaoConfigCommand command) {
        UUID agencyId = command.agencyId();
        if (agencyId == null) {
            throw new IllegalStateException("nenhuma agência no contexto do tenant");
        }

        SolicitacaoConfigRequestDTO request = command.request();
        SolicitacaoConfig config = repository.findFirstByAgency_Id(agencyId)
                .orElseGet(() -> {
                    Agency agency = agencyRepository
                            .findById(agencyId)
                            .orElseThrow(() -> new ResourceNotFoundException("agência não encontrada: " + agencyId));
                    return SolicitacaoConfig.builder()
                            .agency(agency)
                            .slug(request.slug())
                            .build();
                });

        config.setSlug(request.slug());
        config.setTituloPagina(request.tituloPagina());
        config.setTextoIntro(request.textoIntro() != null ? request.textoIntro() : "");
        config.setLogoDataUrl(request.logoDataUrl());
        config.setNomeMarca(
                request.nomeMarca() != null && !request.nomeMarca().isBlank() ? request.nomeMarca() : "Agência");
        config.setLinksSociais(
                request.linksSociais() != null ? request.linksSociais() : SolicitacaoConfigSupport.emptyArray(objectMapper));

        config = repository.save(config);
        return SolicitacaoConfigSupport.toResponse(config);
    }
}
