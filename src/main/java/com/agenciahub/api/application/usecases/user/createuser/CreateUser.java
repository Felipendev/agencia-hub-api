package com.agenciahub.api.application.usecases.user.createuser;

import com.agenciahub.api.application.usecases.user.UserResponseMapper;
import com.agenciahub.api.dto.user.CreateUserRequest;
import com.agenciahub.api.dto.user.UserResponse;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.repository.UserRepository;
import com.agenciahub.api.application.usecases.user.PublicLinkCodeSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateUser implements CreateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PublicLinkCodeSupport publicLinkCodeSupport;
    private final UserResponseMapper userResponseMapper;

    @Override
    @Transactional
    public UserResponse execute(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email().trim().toLowerCase())) {
            throw new IllegalArgumentException("este e-mail já está cadastrado");
        }
        User user = User.builder()
                .name(request.name().strip())
                .email(request.email().trim().toLowerCase())
                .publicLinkCode(publicLinkCodeSupport.allocate())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .active(Boolean.TRUE)
                .commissionPct(request.commissionPct())
                .commissionFixed(request.commissionFixed())
                .build();
        return userResponseMapper.toResponse(userRepository.save(user));
    }
}
