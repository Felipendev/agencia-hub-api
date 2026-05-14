package com.agenciahub.api.application.auth;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.dto.auth.VerifyEmailRequest;
import com.agenciahub.api.dto.auth.VerifyEmailResponse;

public interface VerifyEmailUseCase extends UseCase<VerifyEmailRequest, VerifyEmailResponse> {
}
