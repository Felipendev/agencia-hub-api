package com.agenciahub.api.application.auth;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.dto.auth.InviteValidationResponse;

public interface ValidateInviteTokenUseCase extends UseCase<String, InviteValidationResponse> {
}
