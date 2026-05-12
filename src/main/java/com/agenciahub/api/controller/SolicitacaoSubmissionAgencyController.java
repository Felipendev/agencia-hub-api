package com.agenciahub.api.controller;

import com.agenciahub.api.dto.solicitacao.SolicitacaoSubmissionResponse;
import com.agenciahub.api.service.SolicitacaoSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/agency/solicitacao-submissions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
@Tag(name = "Solicitacao Submissions")
public class SolicitacaoSubmissionAgencyController {

    private final SolicitacaoSubmissionService submissionService;

    @GetMapping
    @Operation(summary = "List public form submissions for the current agency")
    public List<SolicitacaoSubmissionResponse> list() {
        return submissionService.listForCurrentAgency();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a submission after import or discard")
    public void delete(@PathVariable UUID id) {
        submissionService.deleteForCurrentAgency(id);
    }
}
