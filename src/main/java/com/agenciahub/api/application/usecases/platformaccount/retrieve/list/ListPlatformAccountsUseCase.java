package com.agenciahub.api.application.usecases.platformaccount.retrieve.list;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.platformaccount.shared.PlatformAccountSummaryResponseDTO;

import java.util.List;

public interface ListPlatformAccountsUseCase extends UseCase<Void, List<PlatformAccountSummaryResponseDTO>> {
}
