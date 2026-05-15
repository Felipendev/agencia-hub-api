package com.agenciahub.api.application.usecases.salesagent.dashboard.build;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.salesagent.dashboard.build.SalesAgentDashboardResponseDTO;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;

public interface BuildSalesAgentDashboardUseCase extends UseCase<PlatformAccount, SalesAgentDashboardResponseDTO> {
}
