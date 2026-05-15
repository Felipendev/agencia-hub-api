package com.agenciahub.api.controller.solicitacao.agency;

import com.agenciahub.api.application.usecases.solicitacao.deletesolicitacaosubmissionforagency.DeleteSolicitacaoSubmissionCommand;
import com.agenciahub.api.application.usecases.solicitacao.deletesolicitacaosubmissionforagency.DeleteSolicitacaoSubmissionForAgencyUseCase;
import com.agenciahub.api.application.usecases.solicitacao.listsolicitacaosubmissionsforagency.ListSolicitacaoSubmissionsForAgencyUseCase;
import com.agenciahub.api.application.controllers.docs.SolicitacaoSubmissionAgencyAPI;
import com.agenciahub.api.dto.solicitacao.SolicitacaoSubmissionResponse;
import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class SolicitacaoSubmissionAgencyController implements SolicitacaoSubmissionAgencyAPI {

    private final ListSolicitacaoSubmissionsForAgencyUseCase listSolicitacaoSubmissionsForAgencyUseCase;
    private final DeleteSolicitacaoSubmissionForAgencyUseCase deleteSolicitacaoSubmissionForAgencyUseCase;

    @Override
    public List<SolicitacaoSubmissionResponse> list() {
        UUID agencyId = TenantContext.requireAgencyId();
        return listSolicitacaoSubmissionsForAgencyUseCase.execute(agencyId);
    }

    @Override
    public void delete(UUID id) {
        UUID agencyId = TenantContext.requireAgencyId();
        deleteSolicitacaoSubmissionForAgencyUseCase.execute(
                new DeleteSolicitacaoSubmissionCommand(id, agencyId));
    }
}
