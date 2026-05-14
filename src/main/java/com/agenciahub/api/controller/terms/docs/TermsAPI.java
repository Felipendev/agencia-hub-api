package com.agenciahub.api.controller.terms.docs;

import com.agenciahub.api.dto.terms.AcceptTermsRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Map;

@Tag(name = "Termos", description = "Metadados públicos da versão vigente e aceite autenticado (atualiza flag no usuário).")
public interface TermsAPI {

    @GetMapping("/public/terms/latest")
    @Operation(summary = "Versão atual dos termos (público)", description = "Versão, título e URL de referência.")
    Map<String, String> getLatest();

    @PostMapping("/terms/accept")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra aceite", description = "Define `termsAccepted` no usuário autenticado após validar a versão.")
    Map<String, String> accept(@Valid @RequestBody AcceptTermsRequest request);
}
