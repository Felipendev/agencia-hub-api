package com.agenciahub.api.application.usecases.user.retrieve.listactive;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.user.shared.UserSummaryResponseDTO;

import java.util.List;

public interface ListActiveSellersUseCase extends UseCase<Void, List<UserSummaryResponseDTO>> {
}
