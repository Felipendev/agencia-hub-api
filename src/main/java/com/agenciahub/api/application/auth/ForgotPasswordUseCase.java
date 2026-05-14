package com.agenciahub.api.application.auth;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.dto.auth.ForgotPasswordRequest;

import java.util.Map;

public interface ForgotPasswordUseCase extends UseCase<ForgotPasswordRequest, Map<String, String>> {
}
