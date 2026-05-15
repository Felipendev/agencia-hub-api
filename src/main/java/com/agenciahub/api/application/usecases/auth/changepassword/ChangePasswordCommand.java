package com.agenciahub.api.application.usecases.auth.changepassword;

import com.agenciahub.api.dto.auth.ChangePasswordRequest;

import java.util.UUID;

public record ChangePasswordCommand(UUID userId, ChangePasswordRequest request) {
}
