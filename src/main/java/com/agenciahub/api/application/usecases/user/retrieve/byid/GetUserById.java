package com.agenciahub.api.application.usecases.user.retrieve.byid;

import com.agenciahub.api.application.usecases.user.shared.UserResponseMapper;
import com.agenciahub.api.application.usecases.user.shared.UserSummaryResponseDTO;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetUserById implements GetUserByIdUseCase {

    private final UserRepository userRepository;
    private final UserResponseMapper userResponseMapper;

    @Override
    public UserSummaryResponseDTO execute(UUID id) {
        return userRepository
                .findById(id)
                .map(userResponseMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("usuário não encontrado: " + id));
    }
}
