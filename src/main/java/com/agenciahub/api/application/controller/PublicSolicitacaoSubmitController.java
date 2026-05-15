package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.solicitacao.pub.submit.SubmitPublicSolicitacaoUseCase;
import com.agenciahub.api.application.controller.doc.PublicSolicitacaoSubmitAPI;
import com.agenciahub.api.application.usecases.solicitacao.pub.submit.PublicSolicitacaoSubmitRequestDTO;
import com.agenciahub.api.application.usecases.solicitacao.pub.submit.PublicSolicitacaoSubmitResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PublicSolicitacaoSubmitController implements PublicSolicitacaoSubmitAPI {

    private final SubmitPublicSolicitacaoUseCase submitPublicSolicitacaoUseCase;

    @Override
    public PublicSolicitacaoSubmitResponseDTO submit(PublicSolicitacaoSubmitRequestDTO body) {
        return submitPublicSolicitacaoUseCase.execute(body);
    }
}
