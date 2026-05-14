package com.agenciahub.api.application.user;

import com.agenciahub.api.dto.user.UpdateUserRequest;

import java.util.UUID;

public record UpdateUserCommand(UUID id, UpdateUserRequest request) {
}
