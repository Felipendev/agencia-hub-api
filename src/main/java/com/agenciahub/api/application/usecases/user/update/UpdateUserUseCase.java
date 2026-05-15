package com.agenciahub.api.application.usecases.user.update;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.user.shared.UserSummaryResponseDTO;

public interface UpdateUserUseCase extends UseCase<UpdateUserCommand, UserSummaryResponseDTO> {
}
