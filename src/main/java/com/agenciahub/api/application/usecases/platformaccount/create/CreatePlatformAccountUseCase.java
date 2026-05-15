package com.agenciahub.api.application.usecases.platformaccount.create;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.platformaccount.create.CreatePlatformAccountRequestDTO;
import com.agenciahub.api.application.usecases.platformaccount.shared.PlatformAccountSummaryResponseDTO;

public interface CreatePlatformAccountUseCase extends UseCase<CreatePlatformAccountRequestDTO, PlatformAccountSummaryResponseDTO> {
}
