package com.agenciahub.api.application.usecases.platformaccount.retrieve.list;

import com.agenciahub.api.application.usecases.platformaccount.shared.PlatformAccountResponseMapper;
import com.agenciahub.api.application.usecases.platformaccount.shared.PlatformAccountSummaryResponseDTO;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListPlatformAccounts implements ListPlatformAccountsUseCase {

    private final PlatformAccountRepository userRepository;
    private final PlatformAccountResponseMapper userResponseMapper;

    @Override
    public List<PlatformAccountSummaryResponseDTO> execute(Void unused) {
        return userRepository.findAllByOrderByNameAsc().stream()
                .map(userResponseMapper::toResponse)
                .toList();
    }
}
