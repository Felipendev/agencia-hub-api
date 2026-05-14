package com.agenciahub.api.application.solicitacao;

import com.agenciahub.api.dto.solicitacao.SolicitacaoSubmissionResponse;
import com.agenciahub.api.entity.SolicitacaoSubmission;
import com.agenciahub.api.entity.User;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SolicitacaoSubmissionResponseMapper {

    public SolicitacaoSubmissionResponse toResponse(SolicitacaoSubmission s) {
        User ref = s.getReferralSeller();
        UUID refId = ref != null ? ref.getId() : null;
        String refName = ref != null ? ref.getName() : null;
        return new SolicitacaoSubmissionResponse(
                s.getId(),
                s.getSlug(),
                s.getCreatedAt(),
                s.getNome(),
                s.getEmail(),
                s.getTelefone(),
                refId,
                refName,
                s.getDetalhes(),
                s.getObservacoes());
    }
}
