package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.solicitacao.submission.delete.DeleteSolicitacaoSubmissionCommand;
import com.agenciahub.api.application.usecases.solicitacao.submission.delete.DeleteSolicitacaoSubmissionForAgencyUseCase;
import com.agenciahub.api.application.usecases.solicitacao.submission.retrieve.list.ListSolicitacaoSubmissionsForAgencyUseCase;
import com.agenciahub.api.application.usecases.solicitacao.submission.retrieve.list.ListSubmissionsQuery;
import com.agenciahub.api.application.controller.doc.SolicitacaoSubmissionAgencyAPI;
import com.agenciahub.api.application.usecases.solicitacao.shared.SolicitacaoSubmissionSummaryResponseDTO;
import com.agenciahub.api.security.SecurityContextUsers;
import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('AGENCY_OWNER','SALES_AGENT')")
public class SolicitacaoSubmissionAgencyController implements SolicitacaoSubmissionAgencyAPI {

    private final ListSolicitacaoSubmissionsForAgencyUseCase listSolicitacaoSubmissionsForAgencyUseCase;
    private final DeleteSolicitacaoSubmissionForAgencyUseCase deleteSolicitacaoSubmissionForAgencyUseCase;

    @Override
    public List<SolicitacaoSubmissionSummaryResponseDTO> list() {
        UUID agencyId = TenantContext.requireAgencyId();
        var currentUser = SecurityContextUsers.requireUser();
        var query = new ListSubmissionsQuery(agencyId, currentUser.getId(), currentUser.getAccountKind());
        return listSolicitacaoSubmissionsForAgencyUseCase.execute(query);
    }

    @Override
    public void delete(UUID id) {
        UUID agencyId = TenantContext.requireAgencyId();
        deleteSolicitacaoSubmissionForAgencyUseCase.execute(
                new DeleteSolicitacaoSubmissionCommand(id, agencyId));
    }
}
