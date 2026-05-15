package com.agenciahub.api.application.usecases.salesagent.retrieve.listactive;

import com.agenciahub.api.application.usecases.user.shared.UserResponseMapper;
import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.application.usecases.user.shared.UserSummaryResponseDTO;
import com.agenciahub.api.repository.PlatformAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListActiveSalesAgents implements ListActiveSalesAgentsUseCase {

    private final PlatformAccountRepository userRepository;
    private final UserResponseMapper userResponseMapper;

    @Override
    public List<UserSummaryResponseDTO> execute(Void unused) {
        return userRepository.findByRoleAndActiveTrue(AccountKind.SALES_AGENT).stream()
                .map(userResponseMapper::toResponse)
                .toList();
    }
}
