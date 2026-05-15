package com.agenciahub.api.application.usecases.solicitacao.shared;

import com.agenciahub.api.application.usecases.solicitacao.shared.SolicitacaoSubmissionSummaryResponseDTO;
import com.agenciahub.api.application.persistence.entity.SolicitacaoSubmission;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SolicitacaoSubmissionResponseMapper {

    public SolicitacaoSubmissionSummaryResponseDTO toResponse(SolicitacaoSubmission s) {
        PlatformAccount ref = s.getReferralSeller();
        UUID refId = ref != null ? ref.getId() : null;
        String refName = ref != null ? ref.getName() : null;
        return new SolicitacaoSubmissionSummaryResponseDTO(
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
