package com.agenciahub.api.application.usecases.platformaccount.retrieve.byid;

import com.agenciahub.api.application.usecases.platformaccount.shared.PlatformAccountResponseMapper;
import com.agenciahub.api.application.usecases.platformaccount.shared.PlatformAccountSummaryResponseDTO;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetPlatformAccountById implements GetPlatformAccountByIdUseCase {

    private final PlatformAccountRepository userRepository;
    private final PlatformAccountResponseMapper userResponseMapper;

    @Override
    public PlatformAccountSummaryResponseDTO execute(UUID id) {
        return userRepository
                .findById(id)
                .map(userResponseMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("usuário não encontrado: " + id));
    }
}
