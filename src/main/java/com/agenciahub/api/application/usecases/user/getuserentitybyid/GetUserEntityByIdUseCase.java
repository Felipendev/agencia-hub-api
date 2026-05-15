package com.agenciahub.api.application.usecases.user.getuserentitybyid;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.entity.User;

import java.util.UUID;

public interface GetUserEntityByIdUseCase extends UseCase<UUID, User> {}
