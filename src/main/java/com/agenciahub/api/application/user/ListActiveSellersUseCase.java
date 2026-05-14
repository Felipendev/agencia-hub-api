package com.agenciahub.api.application.user;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.dto.user.UserResponse;

import java.util.List;

public interface ListActiveSellersUseCase extends UseCase<Void, List<UserResponse>> {
}
