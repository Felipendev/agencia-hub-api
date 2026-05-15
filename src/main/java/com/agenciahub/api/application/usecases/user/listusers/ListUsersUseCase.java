package com.agenciahub.api.application.usecases.user.listusers;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.dto.user.UserResponse;

import java.util.List;

public interface ListUsersUseCase extends UseCase<Void, List<UserResponse>> {
}
