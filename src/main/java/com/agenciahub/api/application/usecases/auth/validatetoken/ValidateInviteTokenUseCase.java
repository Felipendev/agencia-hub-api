package com.agenciahub.api.application.usecases.auth.validatetoken;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.dto.auth.InviteValidationResponse;

public interface ValidateInviteTokenUseCase extends UseCase<String, InviteValidationResponse> {
}
