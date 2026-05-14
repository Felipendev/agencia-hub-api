package com.agenciahub.api.application.auth;

import com.agenciahub.api.application.UseCase;

import java.util.Map;

public interface ChangePasswordUseCase extends UseCase<ChangePasswordCommand, Map<String, String>> {
}
