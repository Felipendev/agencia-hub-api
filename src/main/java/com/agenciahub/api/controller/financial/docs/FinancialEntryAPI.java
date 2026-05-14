package com.agenciahub.api.controller.financial.docs;

import com.agenciahub.api.api.docs.StandardErrorApiResponses;
import com.agenciahub.api.domain.FinancialEntryCategory;
import com.agenciahub.api.domain.FinancialEntryStatus;
import com.agenciahub.api.domain.FinancialEntryType;
import com.agenciahub.api.dto.financial.CreateFinancialEntryRequest;
import com.agenciahub.api.dto.financial.FinancialEntryResponse;
import com.agenciahub.api.dto.financial.UpdateFinancialEntryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(
        name = "Lançamentos financeiros",
        description = """
                Listagem, consulta, criação e atualização parcial de lançamentos financeiros da agência.

                Acesso restrito a usuários com papel **OWNER**.""")
@RequestMapping("/financial-entries")
@StandardErrorApiResponses
public interface FinancialEntryAPI {

    @GetMapping
    @Operation(
            summary = "Lista lançamentos com filtros opcionais",
            description = """
                    Filtros combináveis: intervalo de datas (`from`/`to`), tipo, categoria, status, cliente e conta bancária.

                    Ordenação: data do lançamento (`entryDate`) decrescente.""")
    List<FinancialEntryResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) FinancialEntryType type,
            @RequestParam(required = false) FinancialEntryCategory category,
            @RequestParam(required = false) FinancialEntryStatus status,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) String bankAccount);

    @GetMapping("/{id}")
    @Operation(summary = "Obtém lançamento por id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "lançamento encontrado"),
            @ApiResponse(responseCode = "404", description = "lançamento inexistente")
    })
    FinancialEntryResponse get(@Parameter(description = "id do lançamento") @PathVariable UUID id);

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Cria lançamento",
            description = "Cliente é opcional; quando informado, deve existir na base.")
    FinancialEntryResponse create(@Valid @RequestBody CreateFinancialEntryRequest request);

    @PatchMapping("/{id}")
    @Operation(summary = "Atualiza parcialmente o lançamento")
    FinancialEntryResponse patch(
            @Parameter(description = "id do lançamento") @PathVariable UUID id,
            @RequestBody UpdateFinancialEntryRequest request);
}
