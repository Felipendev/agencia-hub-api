package com.agenciahub.api.application.solicitacao;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.dto.solicitacao.PublicSolicitacaoSubmitRequest;
import com.agenciahub.api.dto.solicitacao.PublicSolicitacaoSubmitResponse;

public interface SubmitPublicSolicitacaoUseCase
        extends UseCase<PublicSolicitacaoSubmitRequest, PublicSolicitacaoSubmitResponse> {
}
