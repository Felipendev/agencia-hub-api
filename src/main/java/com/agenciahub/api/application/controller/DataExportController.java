package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.application.usecases.export.DataExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
@Tag(name = "Portabilidade de Dados", description = "Exportação LGPD dos dados do titular (art. 18, V).")
public class DataExportController {

    private final DataExportService dataExportService;

    @GetMapping("/data-export")
    @Operation(summary = "Exporta todos os dados do titular em formato ZIP (LGPD art. 18, V)")
    public ResponseEntity<byte[]> dataExport(@AuthenticationPrincipal PlatformAccount caller) throws Exception {
        byte[] zip = dataExportService.isOwner(caller)
                ? dataExportService.buildOwnerExport(caller)
                : dataExportService.buildAgentExport(caller);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"data-export.zip\"")
                .body(zip);
    }
}
