package com.agenciahub.api.application.usecases.user.retrieve.list;

import com.agenciahub.api.application.usecases.user.shared.UserResponseMapper;
import com.agenciahub.api.application.usecases.user.shared.UserSummaryResponseDTO;
import com.agenciahub.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListUsers implements ListUsersUseCase {

    private final UserRepository userRepository;
    private final UserResponseMapper userResponseMapper;

    @Override
    public List<UserSummaryResponseDTO> execute(Void unused) {
        return userRepository.findAllByOrderByNameAsc().stream()
                .map(userResponseMapper::toResponse)
                .toList();
    }
}
