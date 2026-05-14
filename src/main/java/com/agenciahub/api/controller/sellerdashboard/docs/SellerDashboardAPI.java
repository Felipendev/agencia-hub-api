package com.agenciahub.api.controller.sellerdashboard.docs;

import com.agenciahub.api.api.docs.StandardErrorApiResponses;
import com.agenciahub.api.dto.seller.SellerDashboardResponse;
import com.agenciahub.api.entity.User;
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
    SellerDashboardResponse myDashboard(@AuthenticationPrincipal User caller);

    @GetMapping("/{sellerId}")
    @Operation(summary = "Painel de um vendedor (apenas owner)")
    SellerDashboardResponse sellerDashboard(
            @Parameter(description = "id do vendedor") @PathVariable UUID sellerId,
            @AuthenticationPrincipal User caller);
}
