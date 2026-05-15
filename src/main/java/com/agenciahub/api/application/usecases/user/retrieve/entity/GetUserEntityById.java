package com.agenciahub.api.application.usecases.user.retrieve.entity;

import com.agenciahub.api.entity.User;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetUserEntityById implements GetUserEntityByIdUseCase {

    private final UserRepository userRepository;

    @Override
    public User execute(UUID id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("usuário não encontrado: " + id));
    }
}
