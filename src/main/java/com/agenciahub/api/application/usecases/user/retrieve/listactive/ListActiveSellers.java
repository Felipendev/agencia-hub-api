package com.agenciahub.api.application.usecases.user.retrieve.listactive;

import com.agenciahub.api.application.usecases.user.shared.UserResponseMapper;
import com.agenciahub.api.domain.UserRole;
import com.agenciahub.api.application.usecases.user.shared.UserSummaryResponseDTO;
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
    public List<UserSummaryResponseDTO> execute(Void unused) {
        return userRepository.findByRoleAndActiveTrue(UserRole.SELLER).stream()
                .map(userResponseMapper::toResponse)
                .toList();
    }
}
