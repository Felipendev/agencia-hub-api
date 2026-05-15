package com.agenciahub.api.application.usecases.user.updateuser;

import com.agenciahub.api.dto.user.UpdateUserRequest;

import java.util.UUID;

public record UpdateUserCommand(UUID id, UpdateUserRequest request) {
}
