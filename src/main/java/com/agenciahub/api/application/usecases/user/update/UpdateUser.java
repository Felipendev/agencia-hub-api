package com.agenciahub.api.application.usecases.user.update;

import com.agenciahub.api.application.usecases.user.shared.UserResponseMapper;
import com.agenciahub.api.application.usecases.user.update.UpdateUserRequestDTO;
import com.agenciahub.api.application.usecases.user.shared.UserSummaryResponseDTO;
import com.agenciahub.api.entity.PlatformAccount;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.PlatformAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateUser implements UpdateUserUseCase {

    private final PlatformAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserResponseMapper userResponseMapper;

    @Override
    public UserSummaryResponseDTO execute(UpdateUserCommand command) {
        PlatformAccount user = userRepository
                .findById(command.id())
                .orElseThrow(() -> new ResourceNotFoundException("usuário não encontrado: " + command.id()));
        UpdateUserRequestDTO request = command.request();

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
