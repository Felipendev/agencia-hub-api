package com.agenciahub.api.application.usecases.platformaccount.retrieve.entity;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;

import java.util.UUID;

public interface GetPlatformAccountEntityByIdUseCase extends UseCase<UUID, PlatformAccount> {}
