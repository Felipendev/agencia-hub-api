package com.agenciahub.api.application.auth;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.dto.auth.ResendCodeRequest;

import java.util.Map;

public interface ResendCodeUseCase extends UseCase<ResendCodeRequest, Map<String, String>> {
}
