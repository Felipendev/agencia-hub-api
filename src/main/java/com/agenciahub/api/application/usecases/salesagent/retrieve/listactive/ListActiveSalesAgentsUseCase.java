package com.agenciahub.api.application.usecases.salesagent.retrieve.listactive;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.platformaccount.shared.PlatformAccountSummaryResponseDTO;

import java.util.List;

public interface ListActiveSalesAgentsUseCase extends UseCase<Void, List<PlatformAccountSummaryResponseDTO>> {
}
