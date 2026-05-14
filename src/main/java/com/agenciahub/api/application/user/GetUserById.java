package com.agenciahub.api.application.user;

import com.agenciahub.api.dto.user.UserResponse;
import com.agenciahub.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetUserById implements GetUserByIdUseCase {

    private final UserService userService;

    @Override
    public UserResponse execute(UUID id) {
        return userService.getById(id);
    }
}
