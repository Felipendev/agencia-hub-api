package com.agenciahub.api.application.usecases.user.update;

import com.agenciahub.api.application.usecases.user.update.UpdateUserRequestDTO;

import java.util.UUID;

public record UpdateUserCommand(UUID id, UpdateUserRequestDTO request) {
}
