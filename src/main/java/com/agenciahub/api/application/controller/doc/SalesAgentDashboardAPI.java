package com.agenciahub.api.application.controller.doc;

import com.agenciahub.api.application.usecases.salesagent.dashboard.build.SalesAgentDashboardResponseDTO;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@RequestMapping("/sales-agent/dashboard")
@Tag(
        name = "Painel do agente de venda",
        description = "Métricas e cotações recentes do agente autenticado; dono da agência pode consultar qualquer agente.")
@StandardErrorApiResponses
public interface SalesAgentDashboardAPI {

    @GetMapping("/me")
    @Operation(summary = "Painel do agente autenticado")
    SalesAgentDashboardResponseDTO myDashboard(@AuthenticationPrincipal PlatformAccount caller);

    @GetMapping("/{agentId}")
    @Operation(summary = "Painel de um agente (apenas AGENCY_OWNER)")
    SalesAgentDashboardResponseDTO agentDashboard(
            @Parameter(description = "id do agente de venda") @PathVariable UUID agentId,
            @AuthenticationPrincipal PlatformAccount caller);
}
