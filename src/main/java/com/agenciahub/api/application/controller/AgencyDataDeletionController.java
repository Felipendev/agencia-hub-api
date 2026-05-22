package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.application.usecases.datadeletion.ProcessDataDeletionRequest;
import com.agenciahub.api.application.usecases.datadeletion.ProcessDataDeletionRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/agency/data-deletion-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('AGENCY_OWNER')")
@Tag(name = "LGPD — Exclusão de Dados", description = "Processa solicitações de exclusão de dados (AGENCY_OWNER).")
public class AgencyDataDeletionController {

    private final ProcessDataDeletionRequest processDataDeletionRequest;

    @PostMapping("/{id}/process")
    @Operation(summary = "Processa uma solicitação de exclusão de dados")
    public ResponseEntity<Void> process(
            @PathVariable UUID id,
            @Valid @RequestBody ProcessDataDeletionRequestDTO body,
            @AuthenticationPrincipal PlatformAccount caller) {
        processDataDeletionRequest.execute(id, body, caller);
        return ResponseEntity.ok().build();
    }
}
