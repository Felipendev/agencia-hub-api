package com.agenciahub.api.application.usecases.solicitacao.submission.retrieve.list;

import com.agenciahub.api.application.usecases.solicitacao.shared.SolicitacaoSubmissionResponseMapper;
import com.agenciahub.api.application.usecases.solicitacao.shared.SolicitacaoSubmissionSummaryResponseDTO;
import com.agenciahub.api.repository.SolicitacaoSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListSolicitacaoSubmissionsForAgency implements ListSolicitacaoSubmissionsForAgencyUseCase {

    private final SolicitacaoSubmissionRepository submissionRepository;
    private final SolicitacaoSubmissionResponseMapper solicitacaoSubmissionResponseMapper;

    @Override
    public List<SolicitacaoSubmissionSummaryResponseDTO> execute(UUID agencyId) {
        if (agencyId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "agência não identificada");
        }
        return submissionRepository.findByAgency_IdOrderByCreatedAtDesc(agencyId).stream()
                .map(solicitacaoSubmissionResponseMapper::toResponse)
                .toList();
    }
}
