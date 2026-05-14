package com.agenciahub.api.application.auth;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.dto.auth.RegisterAgencyResponse;
import com.agenciahub.api.dto.auth.RegisterViaInviteRequest;

public interface RegisterViaInviteUseCase extends UseCase<RegisterViaInviteRequest, RegisterAgencyResponse> {
}
