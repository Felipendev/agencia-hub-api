package com.agenciahub.api.application.user;

import com.agenciahub.api.dto.user.CreateUserRequest;
import com.agenciahub.api.dto.user.UserResponse;
import com.agenciahub.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateUser implements CreateUserUseCase {

    private final UserService userService;

    @Override
    public UserResponse execute(CreateUserRequest input) {
        return userService.create(input);
    }
}
