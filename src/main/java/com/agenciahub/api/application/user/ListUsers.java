package com.agenciahub.api.application.user;

import com.agenciahub.api.dto.user.UserResponse;
import com.agenciahub.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListUsers implements ListUsersUseCase {

    private final UserRepository userRepository;
    private final UserResponseMapper userResponseMapper;

    @Override
    public List<UserResponse> execute(Void unused) {
        return userRepository.findAllByOrderByNameAsc().stream()
                .map(userResponseMapper::toResponse)
                .toList();
    }
}
