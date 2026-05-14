package com.agenciahub.api.controller.user;

import com.agenciahub.api.application.user.CreateUserUseCase;
import com.agenciahub.api.application.user.GetUserByIdUseCase;
import com.agenciahub.api.application.user.ListActiveSellersUseCase;
import com.agenciahub.api.application.user.ListUsersUseCase;
import com.agenciahub.api.application.user.UpdateUserCommand;
import com.agenciahub.api.application.user.UpdateUserUseCase;
import com.agenciahub.api.controller.user.docs.UserAPI;
import com.agenciahub.api.dto.user.CreateUserRequest;
import com.agenciahub.api.dto.user.UpdateUserRequest;
import com.agenciahub.api.dto.user.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController implements UserAPI {

    private final ListUsersUseCase listUsersUseCase;
    private final ListActiveSellersUseCase listActiveSellersUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final CreateUserUseCase createUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;

    @Override
    public List<UserResponse> list() {
        return listUsersUseCase.execute(null);
    }

    @Override
    public List<UserResponse> sellers() {
        return listActiveSellersUseCase.execute(null);
    }

    @Override
    public UserResponse get(UUID id) {
        return getUserByIdUseCase.execute(id);
    }

    @Override
    public UserResponse create(CreateUserRequest request) {
        return createUserUseCase.execute(request);
    }

    @Override
    public UserResponse patch(UUID id, UpdateUserRequest request) {
        return updateUserUseCase.execute(new UpdateUserCommand(id, request));
    }
}
