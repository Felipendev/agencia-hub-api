package com.agenciahub.api.application.usecases.user.retrieve.list;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.user.shared.UserSummaryResponseDTO;

import java.util.List;

public interface ListUsersUseCase extends UseCase<Void, List<UserSummaryResponseDTO>> {
}
