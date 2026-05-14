package com.agenciahub.api.application.user;

import com.agenciahub.api.dto.user.UserResponse;
import com.agenciahub.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateUser implements UpdateUserUseCase {

    private final UserService userService;

    @Override
    public UserResponse execute(UpdateUserCommand command) {
        return userService.update(command.id(), command.request());
    }
}
