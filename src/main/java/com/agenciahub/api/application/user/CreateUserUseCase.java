package com.agenciahub.api.application.user;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.dto.user.CreateUserRequest;
import com.agenciahub.api.dto.user.UserResponse;

public interface CreateUserUseCase extends UseCase<CreateUserRequest, UserResponse> {
}
