package com.agenciahub.api.application.usecases.auth.registeragency;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.auth.registeragency.RegisterAgencyRequestDTO;
import com.agenciahub.api.application.usecases.auth.shared.RegisterAgencyResultDTO;

public interface RegisterAgencyUseCase extends UseCase<RegisterAgencyRequestDTO, RegisterAgencyResultDTO> {
}
