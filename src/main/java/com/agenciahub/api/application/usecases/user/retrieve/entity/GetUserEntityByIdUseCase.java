package com.agenciahub.api.application.usecases.user.retrieve.entity;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.entity.PlatformAccount;

import java.util.UUID;

public interface GetUserEntityByIdUseCase extends UseCase<UUID, PlatformAccount> {}
