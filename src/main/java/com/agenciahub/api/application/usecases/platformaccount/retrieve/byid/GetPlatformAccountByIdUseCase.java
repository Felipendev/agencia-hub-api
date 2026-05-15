package com.agenciahub.api.application.usecases.platformaccount.retrieve.byid;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.platformaccount.shared.PlatformAccountSummaryResponseDTO;

import java.util.UUID;

public interface GetPlatformAccountByIdUseCase extends UseCase<UUID, PlatformAccountSummaryResponseDTO> {
}
