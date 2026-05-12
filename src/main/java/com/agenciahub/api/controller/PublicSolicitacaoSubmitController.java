package com.agenciahub.api.controller;

import com.agenciahub.api.dto.solicitacao.PublicSolicitacaoSubmitRequest;
import com.agenciahub.api.dto.solicitacao.PublicSolicitacaoSubmitResponse;
import com.agenciahub.api.service.SolicitacaoSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/solicitacao")
@RequiredArgsConstructor
@Tag(name = "Public Solicitacao")
public class PublicSolicitacaoSubmitController {

    private final SolicitacaoSubmissionService submissionService;

    @PostMapping("/submit")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Persist public quotation form submission (no auth)")
    public PublicSolicitacaoSubmitResponse submit(@Valid @RequestBody PublicSolicitacaoSubmitRequest body) {
        return submissionService.submit(body);
    }
}
