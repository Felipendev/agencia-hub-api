package com.agenciahub.api.application.solicitacao;

import com.agenciahub.api.dto.solicitacao.SolicitacaoSubmissionResponse;
import com.agenciahub.api.service.SolicitacaoSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListSolicitacaoSubmissionsForAgency implements ListSolicitacaoSubmissionsForAgencyUseCase {

    private final SolicitacaoSubmissionService solicitacaoSubmissionService;

    @Override
    public List<SolicitacaoSubmissionResponse> execute(UUID agencyId) {
        return solicitacaoSubmissionService.listForAgency(agencyId);
    }
}
