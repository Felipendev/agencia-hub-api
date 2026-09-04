package com.agenciahub.api.application.usecases.solicitacao.pub.retrieve.byslug;

import com.agenciahub.api.application.usecases.solicitacao.shared.SolicitacaoConfigSupport;
import com.agenciahub.api.application.usecases.solicitacao.shared.SolicitacaoConfigSummaryResponseDTO;
import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.entity.SolicitacaoConfig;
import com.agenciahub.api.application.persistence.repository.SolicitacaoConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetPublicSolicitacaoConfigBySlug implements GetPublicSolicitacaoConfigBySlugUseCase {

    private final SolicitacaoConfigRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public SolicitacaoConfigSummaryResponseDTO execute(String slug) {
        return repository
                .findFirstBySlug(slug)
                .map(config -> {
                    SolicitacaoConfigSummaryResponseDTO response = SolicitacaoConfigSupport.toResponse(config);
                    Agency agency = config.getAgency();
                    return SolicitacaoConfigSupport.withAgencyLogoFallback(response, agency);
                })
                .orElse(SolicitacaoConfigSupport.defaultResponse(slug, objectMapper));
    }
}
