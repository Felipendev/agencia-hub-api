package com.agenciahub.api.application.usecases.user.retrieve.entity;

import com.agenciahub.api.entity.PlatformAccount;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.PlatformAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetUserEntityById implements GetUserEntityByIdUseCase {

    private final PlatformAccountRepository userRepository;

    @Override
    public PlatformAccount execute(UUID id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("usuário não encontrado: " + id));
    }
}
