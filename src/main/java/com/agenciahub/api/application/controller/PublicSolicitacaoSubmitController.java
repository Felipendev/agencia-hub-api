package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.solicitacao.submitpublicsolicitacao.SubmitPublicSolicitacaoUseCase;
import com.agenciahub.api.application.controller.doc.PublicSolicitacaoSubmitAPI;
import com.agenciahub.api.dto.solicitacao.PublicSolicitacaoSubmitRequest;
import com.agenciahub.api.dto.solicitacao.PublicSolicitacaoSubmitResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PublicSolicitacaoSubmitController implements PublicSolicitacaoSubmitAPI {

    private final SubmitPublicSolicitacaoUseCase submitPublicSolicitacaoUseCase;

    @Override
    public PublicSolicitacaoSubmitResponse submit(PublicSolicitacaoSubmitRequest body) {
        return submitPublicSolicitacaoUseCase.execute(body);
    }
}
