package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.solicitacao.config.retrieve.GetOrCreateSolicitacaoConfigForAgencyUseCase;
import com.agenciahub.api.application.usecases.solicitacao.config.upsert.UpsertSolicitacaoConfigCommand;
import com.agenciahub.api.application.usecases.solicitacao.config.upsert.UpsertSolicitacaoConfigForAgencyUseCase;
import com.agenciahub.api.application.controller.doc.SolicitacaoConfigAgencyAPI;
import com.agenciahub.api.application.usecases.solicitacao.config.upsert.SolicitacaoConfigRequestDTO;
import com.agenciahub.api.application.usecases.solicitacao.shared.SolicitacaoConfigSummaryResponseDTO;
import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('AGENCY_OWNER')")
public class SolicitacaoConfigController implements SolicitacaoConfigAgencyAPI {

    private final GetOrCreateSolicitacaoConfigForAgencyUseCase getOrCreateSolicitacaoConfigForAgencyUseCase;
    private final UpsertSolicitacaoConfigForAgencyUseCase upsertSolicitacaoConfigForAgencyUseCase;

    @Override
    public SolicitacaoConfigSummaryResponseDTO get() {
        UUID agencyId = TenantContext.requireAgencyId();
        return getOrCreateSolicitacaoConfigForAgencyUseCase.execute(agencyId);
    }

    @Override
    public SolicitacaoConfigSummaryResponseDTO upsert(SolicitacaoConfigRequestDTO request) {
        UUID agencyId = TenantContext.requireAgencyId();
        return upsertSolicitacaoConfigForAgencyUseCase.execute(
                new UpsertSolicitacaoConfigCommand(agencyId, request));
    }
}
