package com.agenciahub.api.application.usecases.solicitacao.submission.update;

import com.agenciahub.api.application.persistence.repository.SolicitacaoSubmissionRepository;
import com.agenciahub.api.domain.SolicitacaoSubmissionStatus;
import com.agenciahub.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateSolicitacaoSubmissionStatus {
    private final SolicitacaoSubmissionRepository submissionRepository;

    @Transactional
    public void execute(UUID id, UUID agencyId, SolicitacaoSubmissionStatus requested) {
        var submission = submissionRepository.findWithLockByIdAndAgency_Id(id, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("submissão não encontrada: " + id));
        if (submission.getStatus() == SolicitacaoSubmissionStatus.CONVERTED && requested != SolicitacaoSubmissionStatus.DELETED) {
            throw new IllegalStateException("Uma solicitação convertida não pode voltar para a caixa de entrada.");
        }
        if (requested == SolicitacaoSubmissionStatus.CONVERTED) {
            throw new IllegalArgumentException("Use a criação de cotação para converter a solicitação.");
        }
        submission.setStatus(requested);
        submission.setStatusUpdatedAt(Instant.now());
    }
}
