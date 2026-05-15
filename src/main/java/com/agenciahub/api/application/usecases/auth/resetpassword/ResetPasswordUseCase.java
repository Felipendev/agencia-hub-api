package com.agenciahub.api.application.usecases.auth.resetpassword;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.auth.resetpassword.ResetPasswordRequestDTO;

import java.util.Map;

public interface ResetPasswordUseCase extends UseCase<ResetPasswordRequestDTO, Map<String, String>> {
}
