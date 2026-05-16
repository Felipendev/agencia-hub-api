package com.agenciahub.api.application.usecases.auth.forgotpassword;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.auth.forgotpassword.ForgotPasswordRequestDTO;

import java.util.Map;

public interface ForgotPasswordUseCase extends UseCase<ForgotPasswordRequestDTO, Map<String, String>> {
}
