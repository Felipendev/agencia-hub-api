package com.agenciahub.api.application.usecases.auth.resendcode;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.auth.resendcode.ResendCodeRequestDTO;

import java.util.Map;

public interface ResendCodeUseCase extends UseCase<ResendCodeRequestDTO, Map<String, String>> {
}
