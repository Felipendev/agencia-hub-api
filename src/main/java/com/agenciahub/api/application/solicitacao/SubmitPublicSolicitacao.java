package com.agenciahub.api.application.solicitacao;

import com.agenciahub.api.dto.solicitacao.PublicSolicitacaoSubmitRequest;
import com.agenciahub.api.dto.solicitacao.PublicSolicitacaoSubmitResponse;
import com.agenciahub.api.service.SolicitacaoSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubmitPublicSolicitacao implements SubmitPublicSolicitacaoUseCase {

    private final SolicitacaoSubmissionService solicitacaoSubmissionService;

    @Override
    public PublicSolicitacaoSubmitResponse execute(PublicSolicitacaoSubmitRequest input) {
        return solicitacaoSubmissionService.submit(input);
    }
}
