package com.agenciahub.api.application.solicitacao;

import com.agenciahub.api.service.SolicitacaoSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteSolicitacaoSubmissionForAgency implements DeleteSolicitacaoSubmissionForAgencyUseCase {

    private final SolicitacaoSubmissionService solicitacaoSubmissionService;

    @Override
    public void execute(DeleteSolicitacaoSubmissionCommand command) {
        solicitacaoSubmissionService.deleteForAgency(command.submissionId(), command.agencyId());
    }
}
