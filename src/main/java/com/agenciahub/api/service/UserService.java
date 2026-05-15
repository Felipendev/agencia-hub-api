package com.agenciahub.api.service;

import com.agenciahub.api.application.user.UserResponseMapper;
import com.agenciahub.api.domain.UserRole;
import com.agenciahub.api.dto.user.CreateUserRequest;
import com.agenciahub.api.dto.user.UpdateUserRequest;
import com.agenciahub.api.dto.user.UserResponse;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PublicLinkCodeService publicLinkCodeService;
    private final UserResponseMapper userResponseMapper;

    public List<UserResponse> listAll() {
        return userRepository.findAllByOrderByNameAsc()
                .stream()
                .map(userResponseMapper::toResponse)
                .toList();
    }

    public List<UserResponse> listSellers() {
        return userRepository.findByRoleAndActiveTrue(UserRole.SELLER)
                .stream()
                .map(userResponseMapper::toResponse)
                .toList();
    }

    public UserResponse getById(UUID id) {
        return userRepository.findById(id)
                .map(userResponseMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("usuário não encontrado: " + id));
    }

    public User getEntityById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("usuário não encontrado: " + id));
    }

    /** Transação única: checagem de e-mail + insert evitam corrida óbvia entre chamadas. */
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email().trim().toLowerCase())) {
            throw new IllegalArgumentException("este e-mail já está cadastrado");
        }
        User user = User.builder()
                .name(request.name().strip())
                .email(request.email().trim().toLowerCase())
                .publicLinkCode(publicLinkCodeService.allocate())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .active(Boolean.TRUE)
                .commissionPct(request.commissionPct())
                .commissionFixed(request.commissionFixed())
                .build();
        return userResponseMapper.toResponse(userRepository.save(user));
    }

    public UserResponse update(UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("usuário não encontrado: " + id));

        if (request.name() != null) user.setName(request.name().strip());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        if (request.active() != null) user.setActive(request.active());
        if (request.commissionPct() != null) {
            user.setCommissionPct(request.commissionPct());
            user.setCommissionFixed(null); // mutually exclusive
        }
        if (request.commissionFixed() != null) {
            user.setCommissionFixed(request.commissionFixed());
            user.setCommissionPct(null); // mutually exclusive
        }
        return userResponseMapper.toResponse(userRepository.save(user));
    }
}
