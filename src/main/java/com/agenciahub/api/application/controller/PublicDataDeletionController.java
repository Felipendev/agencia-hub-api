package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.datadeletion.CreateDataDeletionRequest;
import com.agenciahub.api.application.usecases.datadeletion.CreateDataDeletionRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/public/data-deletion-request")
@RequiredArgsConstructor
@Tag(name = "LGPD — Exclusão de Dados", description = "Endpoint público para titulares solicitarem exclusão de dados (LGPD art. 18, VI).")
public class PublicDataDeletionController {

    private final CreateDataDeletionRequest createDataDeletionRequest;

    @PostMapping
    @Operation(summary = "Solicita exclusão de dados pessoais (titular externo)")
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateDataDeletionRequestDTO body) {
        var result = createDataDeletionRequest.execute(body);
        int status = result.created() ? HttpStatus.CREATED.value() : HttpStatus.OK.value();
        return ResponseEntity.status(status)
                .body(Map.of("requestId", result.requestId().toString()));
    }
}
