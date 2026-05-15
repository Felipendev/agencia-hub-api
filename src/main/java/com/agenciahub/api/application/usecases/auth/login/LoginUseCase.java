package com.agenciahub.api.application.usecases.auth.login;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.auth.login.LoginRequestDTO;
import com.agenciahub.api.application.usecases.auth.login.LoginResponseDTO;

public interface LoginUseCase extends UseCase<LoginRequestDTO, LoginResponseDTO> {
}
