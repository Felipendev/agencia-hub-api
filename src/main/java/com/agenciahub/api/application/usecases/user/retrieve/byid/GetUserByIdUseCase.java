package com.agenciahub.api.application.usecases.user.retrieve.byid;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.user.shared.UserSummaryResponseDTO;

import java.util.UUID;

public interface GetUserByIdUseCase extends UseCase<UUID, UserSummaryResponseDTO> {
}
