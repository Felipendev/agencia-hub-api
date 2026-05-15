package com.agenciahub.api.application.usecases.auth.changepassword;

import com.agenciahub.api.application.usecases.auth.changepassword.ChangePasswordRequestDTO;

import java.util.UUID;

public record ChangePasswordCommand(UUID userId, ChangePasswordRequestDTO request) {
}
