package com.agenciahub.api.application.auth;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.dto.auth.ResetPasswordRequest;

import java.util.Map;

public interface ResetPasswordUseCase extends UseCase<ResetPasswordRequest, Map<String, String>> {
}
