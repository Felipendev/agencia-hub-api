package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.controller.doc.SalesAgentDashboardAPI;
import com.agenciahub.api.application.usecases.salesagent.dashboard.build.BuildSalesAgentDashboardUseCase;
import com.agenciahub.api.application.usecases.salesagent.dashboard.build.SalesAgentDashboardResponseDTO;
import com.agenciahub.api.application.usecases.platformaccount.retrieve.entity.GetPlatformAccountEntityByIdUseCase;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class SalesAgentDashboardController implements SalesAgentDashboardAPI {

    private final BuildSalesAgentDashboardUseCase buildSalesAgentDashboardUseCase;
    private final GetPlatformAccountEntityByIdUseCase getUserEntityByIdUseCase;

    @Override
    public SalesAgentDashboardResponseDTO myDashboard(@AuthenticationPrincipal PlatformAccount caller) {
        return buildSalesAgentDashboardUseCase.execute(caller);
    }

    @Override
    @PreAuthorize("hasRole('AGENCY_OWNER')")
    public SalesAgentDashboardResponseDTO agentDashboard(
            UUID agentId,
            @AuthenticationPrincipal PlatformAccount caller) {
        PlatformAccount agent = getUserEntityByIdUseCase.execute(agentId);
        return buildSalesAgentDashboardUseCase.execute(agent);
    }
}
