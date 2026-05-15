package com.agenciahub.api.application.usecases.salesagent.retrieve.listactive;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.user.shared.UserSummaryResponseDTO;

import java.util.List;

public interface ListActiveSalesAgentsUseCase extends UseCase<Void, List<UserSummaryResponseDTO>> {
}
