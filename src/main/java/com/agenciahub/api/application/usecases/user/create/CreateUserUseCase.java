package com.agenciahub.api.application.usecases.user.create;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.user.create.CreateUserRequestDTO;
import com.agenciahub.api.application.usecases.user.shared.UserSummaryResponseDTO;

public interface CreateUserUseCase extends UseCase<CreateUserRequestDTO, UserSummaryResponseDTO> {
}
