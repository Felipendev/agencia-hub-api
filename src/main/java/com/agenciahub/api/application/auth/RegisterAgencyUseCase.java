package com.agenciahub.api.application.auth;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.dto.auth.RegisterAgencyRequest;
import com.agenciahub.api.dto.auth.RegisterAgencyResponse;

public interface RegisterAgencyUseCase extends UseCase<RegisterAgencyRequest, RegisterAgencyResponse> {
}
