package com.agenciahub.api.application.usecases.auth.validatetoken;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.auth.validatetoken.InviteValidationResponseDTO;

public interface ValidateInviteTokenUseCase extends UseCase<String, InviteValidationResponseDTO> {
}
