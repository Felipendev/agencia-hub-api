package com.agenciahub.api.application.usecases.user.listactivesellers;

import com.agenciahub.api.application.usecases.user.UserResponseMapper;
import com.agenciahub.api.domain.UserRole;
import com.agenciahub.api.dto.user.UserResponse;
import com.agenciahub.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListActiveSellers implements ListActiveSellersUseCase {

    private final UserRepository userRepository;
    private final UserResponseMapper userResponseMapper;

    @Override
    public List<UserResponse> execute(Void unused) {
        return userRepository.findByRoleAndActiveTrue(UserRole.SELLER).stream()
                .map(userResponseMapper::toResponse)
                .toList();
    }
}
