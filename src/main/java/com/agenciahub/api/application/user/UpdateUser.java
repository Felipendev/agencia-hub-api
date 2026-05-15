package com.agenciahub.api.application.user;

import com.agenciahub.api.dto.user.UpdateUserRequest;
import com.agenciahub.api.dto.user.UserResponse;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateUser implements UpdateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserResponseMapper userResponseMapper;

    @Override
    public UserResponse execute(UpdateUserCommand command) {
        User user = userRepository
                .findById(command.id())
                .orElseThrow(() -> new ResourceNotFoundException("usuário não encontrado: " + command.id()));
        UpdateUserRequest request = command.request();

        if (request.name() != null) {
            user.setName(request.name().strip());
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        if (request.active() != null) {
            user.setActive(request.active());
        }
        if (request.commissionPct() != null) {
            user.setCommissionPct(request.commissionPct());
            user.setCommissionFixed(null);
        }
        if (request.commissionFixed() != null) {
            user.setCommissionFixed(request.commissionFixed());
            user.setCommissionPct(null);
        }
        return userResponseMapper.toResponse(userRepository.save(user));
    }
}
