package com.agenciahub.api.controller.solicitacao.agency;

import com.agenciahub.api.application.usecases.solicitacao.getorcreatesolicitacaoconfigforagency.GetOrCreateSolicitacaoConfigForAgencyUseCase;
import com.agenciahub.api.application.usecases.solicitacao.upsertsolicitacaoconfigforagency.UpsertSolicitacaoConfigCommand;
import com.agenciahub.api.application.usecases.solicitacao.upsertsolicitacaoconfigforagency.UpsertSolicitacaoConfigForAgencyUseCase;
import com.agenciahub.api.application.controllers.docs.SolicitacaoConfigAgencyAPI;
import com.agenciahub.api.dto.solicitacao.SolicitacaoConfigRequest;
import com.agenciahub.api.dto.solicitacao.SolicitacaoConfigResponse;
import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class SolicitacaoConfigController implements SolicitacaoConfigAgencyAPI {

    private final GetOrCreateSolicitacaoConfigForAgencyUseCase getOrCreateSolicitacaoConfigForAgencyUseCase;
    private final UpsertSolicitacaoConfigForAgencyUseCase upsertSolicitacaoConfigForAgencyUseCase;

    @Override
    public SolicitacaoConfigResponse get() {
        UUID agencyId = TenantContext.requireAgencyId();
        return getOrCreateSolicitacaoConfigForAgencyUseCase.execute(agencyId);
    }

    @Override
    public SolicitacaoConfigResponse upsert(SolicitacaoConfigRequest request) {
        UUID agencyId = TenantContext.requireAgencyId();
        return upsertSolicitacaoConfigForAgencyUseCase.execute(
                new UpsertSolicitacaoConfigCommand(agencyId, request));
    }
}
