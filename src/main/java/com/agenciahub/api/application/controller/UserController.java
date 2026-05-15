package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.user.create.CreateUserUseCase;
import com.agenciahub.api.application.usecases.user.retrieve.byid.GetUserByIdUseCase;
import com.agenciahub.api.application.usecases.salesagent.retrieve.listactive.ListActiveSalesAgentsUseCase;
import com.agenciahub.api.application.usecases.user.retrieve.list.ListUsersUseCase;
import com.agenciahub.api.application.usecases.user.update.UpdateUserCommand;
import com.agenciahub.api.application.usecases.user.update.UpdateUserUseCase;
import com.agenciahub.api.application.controller.doc.UserAPI;
import com.agenciahub.api.application.usecases.user.create.CreateUserRequestDTO;
import com.agenciahub.api.application.usecases.user.update.UpdateUserRequestDTO;
import com.agenciahub.api.application.usecases.user.shared.UserSummaryResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController implements UserAPI {

    private final ListUsersUseCase listUsersUseCase;
    private final ListActiveSalesAgentsUseCase listActiveSalesAgentsUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final CreateUserUseCase createUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;

    @Override
    public List<UserSummaryResponseDTO> list() {
        return listUsersUseCase.execute(null);
    }

    @Override
    public List<UserSummaryResponseDTO> sellers() {
        return listActiveSalesAgentsUseCase.execute(null);
    }

    @Override
    public UserSummaryResponseDTO get(UUID id) {
        return getUserByIdUseCase.execute(id);
    }

    @Override
    public UserSummaryResponseDTO create(CreateUserRequestDTO request) {
        return createUserUseCase.execute(request);
    }

    @Override
    public UserSummaryResponseDTO patch(UUID id, UpdateUserRequestDTO request) {
        return updateUserUseCase.execute(new UpdateUserCommand(id, request));
    }
}
