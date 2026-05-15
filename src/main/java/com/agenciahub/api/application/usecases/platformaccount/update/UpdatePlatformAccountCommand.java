package com.agenciahub.api.application.usecases.platformaccount.update;

import com.agenciahub.api.application.usecases.platformaccount.update.UpdatePlatformAccountRequestDTO;

import java.util.UUID;

public record UpdatePlatformAccountCommand(UUID id, UpdatePlatformAccountRequestDTO request) {
}
