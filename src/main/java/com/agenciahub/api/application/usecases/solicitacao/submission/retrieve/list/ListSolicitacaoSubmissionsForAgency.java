package com.agenciahub.api.application.usecases.solicitacao.submission.retrieve.list;

import com.agenciahub.api.application.usecases.solicitacao.shared.SolicitacaoSubmissionResponseMapper;
import com.agenciahub.api.application.usecases.solicitacao.shared.SolicitacaoSubmissionSummaryResponseDTO;
import com.agenciahub.api.application.persistence.repository.SolicitacaoSubmissionRepository;
import com.agenciahub.api.domain.enums.AccountKind;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListSolicitacaoSubmissionsForAgency implements ListSolicitacaoSubmissionsForAgencyUseCase {

    private final SolicitacaoSubmissionRepository submissionRepository;
    private final SolicitacaoSubmissionResponseMapper solicitacaoSubmissionResponseMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SolicitacaoSubmissionSummaryResponseDTO> execute(ListSubmissionsQuery query) {
        if (query.agencyId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "agência não identificada");
        }
        var rows = query.accountKind() == AccountKind.SALES_AGENT
                ? submissionRepository.findByAgency_IdAndReferralSeller_IdOrderByCreatedAtDesc(
                        query.agencyId(), query.currentUserId())
                : submissionRepository.findByAgency_IdOrderByCreatedAtDesc(query.agencyId());

        return rows.stream()
                .map(solicitacaoSubmissionResponseMapper::toResponse)
                .toList();
    }
}
