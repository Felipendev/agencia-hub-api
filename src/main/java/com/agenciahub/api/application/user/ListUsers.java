package com.agenciahub.api.application.user;

import com.agenciahub.api.dto.user.UserResponse;
import com.agenciahub.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListUsers implements ListUsersUseCase {

    private final UserService userService;

    @Override
    public List<UserResponse> execute(Void unused) {
        return userService.listAll();
    }
}
