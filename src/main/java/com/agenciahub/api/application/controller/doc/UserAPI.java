package com.agenciahub.api.application.controller.doc;

import com.agenciahub.api.application.usecases.platformaccount.create.CreatePlatformAccountRequestDTO;
import com.agenciahub.api.application.usecases.platformaccount.update.UpdatePlatformAccountRequestDTO;
import com.agenciahub.api.application.usecases.platformaccount.shared.PlatformAccountSummaryResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.UUID;

@RequestMapping("/users")
@Tag(
        name = "Usuários",
        description = "Gestão de usuários da agência. Acesso restrito a papel **AGENCY_OWNER** (ver filtro de segurança).")
@StandardErrorApiResponses
public interface UserAPI {

    @GetMapping
    @Operation(summary = "Lista usuários", description = "Ordenação alfabética por nome.")
    List<PlatformAccountSummaryResponseDTO> list();

    @GetMapping("/sales-agents")
    @Operation(summary = "Lista agentes de venda ativos", description = "Usado em combos de atribuição de cotação.")
    List<PlatformAccountSummaryResponseDTO> listSalesAgents();

    @GetMapping("/{id}")
    @Operation(summary = "Obtém usuário por id")
    PlatformAccountSummaryResponseDTO get(@Parameter(description = "id do usuário") @PathVariable UUID id);

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria usuário", description = "Owner ou vendedor conforme corpo da requisição.")
    PlatformAccountSummaryResponseDTO create(@Valid @RequestBody CreatePlatformAccountRequestDTO request);

    @PatchMapping("/{id}")
    @Operation(summary = "Atualiza usuário", description = "Nome, senha, ativo, comissão percentual ou fixa.")
    PlatformAccountSummaryResponseDTO patch(
            @Parameter(description = "id do usuário") @PathVariable UUID id,
            @RequestBody UpdatePlatformAccountRequestDTO request);
}
