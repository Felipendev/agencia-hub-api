package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.platformaccount.create.CreatePlatformAccountUseCase;
import com.agenciahub.api.application.usecases.platformaccount.retrieve.byid.GetPlatformAccountByIdUseCase;
import com.agenciahub.api.application.usecases.salesagent.retrieve.listactive.ListActiveSalesAgentsUseCase;
import com.agenciahub.api.application.usecases.platformaccount.retrieve.list.ListPlatformAccountsUseCase;
import com.agenciahub.api.application.usecases.platformaccount.update.UpdatePlatformAccountCommand;
import com.agenciahub.api.application.usecases.platformaccount.update.UpdatePlatformAccountUseCase;
import com.agenciahub.api.application.controller.doc.UserAPI;
import com.agenciahub.api.application.usecases.platformaccount.create.CreatePlatformAccountRequestDTO;
import com.agenciahub.api.application.usecases.platformaccount.update.UpdatePlatformAccountRequestDTO;
import com.agenciahub.api.application.usecases.platformaccount.shared.PlatformAccountSummaryResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController implements UserAPI {

    private final ListPlatformAccountsUseCase listUsersUseCase;
    private final ListActiveSalesAgentsUseCase listActiveSalesAgentsUseCase;
    private final GetPlatformAccountByIdUseCase getUserByIdUseCase;
    private final CreatePlatformAccountUseCase createUserUseCase;
    private final UpdatePlatformAccountUseCase updateUserUseCase;

    @Override
    public List<PlatformAccountSummaryResponseDTO> list() {
        return listUsersUseCase.execute(null);
    }

    @Override
    public List<PlatformAccountSummaryResponseDTO> listSalesAgents() {
        return listActiveSalesAgentsUseCase.execute(null);
    }

    @Override
    public PlatformAccountSummaryResponseDTO get(UUID id) {
        return getUserByIdUseCase.execute(id);
    }

    @Override
    public PlatformAccountSummaryResponseDTO create(CreatePlatformAccountRequestDTO request) {
        return createUserUseCase.execute(request);
    }

    @Override
    public PlatformAccountSummaryResponseDTO patch(UUID id, UpdatePlatformAccountRequestDTO request) {
        return updateUserUseCase.execute(new UpdatePlatformAccountCommand(id, request));
    }
}
