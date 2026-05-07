package com.agenciahub.api.controller;

import com.agenciahub.api.dto.solicitacao.SolicitacaoConfigResponse;
import com.agenciahub.api.service.SolicitacaoConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/solicitacao-config")
@RequiredArgsConstructor
@Tag(name = "Public Solicitacao")
public class PublicSolicitacaoController {

    private final SolicitacaoConfigService service;

    @GetMapping("/{slug}")
    @Operation(summary = "Get public solicitacao config by slug (no auth required)")
    public SolicitacaoConfigResponse getBySlug(@PathVariable String slug) {
        return service.getPublicBySlug(slug);
    }
}
