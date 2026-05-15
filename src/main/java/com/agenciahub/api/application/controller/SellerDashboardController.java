package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.salesagent.dashboard.build.BuildSalesAgentDashboardUseCase;
import com.agenciahub.api.application.controller.doc.SellerDashboardAPI;
import com.agenciahub.api.application.usecases.user.retrieve.entity.GetUserEntityByIdUseCase;
import com.agenciahub.api.application.usecases.salesagent.dashboard.build.SalesAgentDashboardResponseDTO;
import com.agenciahub.api.entity.PlatformAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class SellerDashboardController implements SellerDashboardAPI {

    private final BuildSalesAgentDashboardUseCase buildSellerDashboardUseCase;
    private final GetUserEntityByIdUseCase getUserEntityByIdUseCase;

    @Override
    public SalesAgentDashboardResponseDTO myDashboard(@AuthenticationPrincipal PlatformAccount caller) {
        return buildSellerDashboardUseCase.execute(caller);
    }

    @Override
    @PreAuthorize("hasRole('AGENCY_OWNER')")
    public SalesAgentDashboardResponseDTO sellerDashboard(
            @PathVariable UUID sellerId,
            @AuthenticationPrincipal PlatformAccount caller) {
        PlatformAccount seller = getUserEntityByIdUseCase.execute(sellerId);
        return buildSellerDashboardUseCase.execute(seller);
    }
}
