package com.agenciahub.api.application.usecases.platformaccount.retrieve.entity;

import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetPlatformAccountEntityById implements GetPlatformAccountEntityByIdUseCase {

    private final PlatformAccountRepository userRepository;

    @Override
    public PlatformAccount execute(UUID id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("usuário não encontrado: " + id));
    }
}
