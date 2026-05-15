package com.agenciahub.api.controller.solicitacao.pub;

import com.agenciahub.api.application.solicitacao.SubmitPublicSolicitacaoUseCase;
import com.agenciahub.api.application.controllers.docs.PublicSolicitacaoSubmitAPI;
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
