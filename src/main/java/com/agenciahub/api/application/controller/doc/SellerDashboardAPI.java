package com.agenciahub.api.application.controller.doc;

import com.agenciahub.api.application.usecases.salesagent.dashboard.build.SalesAgentDashboardResponseDTO;
import com.agenciahub.api.entity.PlatformAccount;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@RequestMapping("/seller-dashboard")
@Tag(name = "Painel do vendedor", description = "Métricas e cotações recentes do vendedor; owner pode consultar qualquer vendedor.")
@StandardErrorApiResponses
public interface SellerDashboardAPI {

    @GetMapping("/me")
    @Operation(summary = "Painel do vendedor autenticado")
    SalesAgentDashboardResponseDTO myDashboard(@AuthenticationPrincipal PlatformAccount caller);

    @GetMapping("/{sellerId}")
    @Operation(summary = "Painel de um vendedor (apenas owner)")
    SalesAgentDashboardResponseDTO sellerDashboard(
            @Parameter(description = "id do vendedor") @PathVariable UUID sellerId,
            @AuthenticationPrincipal PlatformAccount caller);
}
