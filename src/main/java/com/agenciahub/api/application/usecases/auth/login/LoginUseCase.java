package com.agenciahub.api.application.usecases.auth.login;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.dto.auth.LoginRequest;
import com.agenciahub.api.dto.auth.LoginResponse;

public interface LoginUseCase extends UseCase<LoginRequest, LoginResponse> {
}
