package com.agenciahub.api.controller;

import com.agenciahub.api.dto.solicitacao.SolicitacaoConfigRequest;
import com.agenciahub.api.dto.solicitacao.SolicitacaoConfigResponse;
import com.agenciahub.api.service.SolicitacaoConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agency/solicitacao-config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
@Tag(name = "Solicitacao Config")
public class SolicitacaoConfigController {

    private final SolicitacaoConfigService service;

    @GetMapping
    @Operation(summary = "Get solicitacao config for the current agency (creates default if not exists)")
    public SolicitacaoConfigResponse get() {
        return service.getOrCreateForCurrentAgency();
    }

    @PutMapping
    @Operation(summary = "Upsert solicitacao config")
    public SolicitacaoConfigResponse upsert(@Valid @RequestBody SolicitacaoConfigRequest request) {
        return service.upsert(request);
    }
}
