package com.agenciahub.api.application.usecases.solicitacao.submission.delete;

import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.application.persistence.repository.SolicitacaoSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class DeleteSolicitacaoSubmissionForAgency implements DeleteSolicitacaoSubmissionForAgencyUseCase {

    private final SolicitacaoSubmissionRepository submissionRepository;

    @Override
    @Transactional
    public void execute(DeleteSolicitacaoSubmissionCommand command) {
        if (command.agencyId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "agência não identificada");
        }
        var row = submissionRepository
                .findByIdAndAgency_Id(command.submissionId(), command.agencyId())
                .orElseThrow(() -> new ResourceNotFoundException("submissão não encontrada: " + command.submissionId()));
        submissionRepository.delete(row);
    }
}
