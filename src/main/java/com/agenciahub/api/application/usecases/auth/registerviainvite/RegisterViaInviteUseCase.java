package com.agenciahub.api.application.usecases.auth.registerviainvite;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.auth.shared.RegisterAgencyResultDTO;
import com.agenciahub.api.application.usecases.auth.registerviainvite.RegisterViaInviteRequestDTO;

public interface RegisterViaInviteUseCase extends UseCase<RegisterViaInviteRequestDTO, RegisterAgencyResultDTO> {
}
