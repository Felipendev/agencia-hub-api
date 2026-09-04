package com.agenciahub.api.application.usecases.platformaccount.retrieve.list;

import com.agenciahub.api.application.usecases.platformaccount.shared.PlatformAccountResponseMapper;
import com.agenciahub.api.application.usecases.platformaccount.shared.PlatformAccountSummaryResponseDTO;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListPlatformAccounts implements ListPlatformAccountsUseCase {

    private final PlatformAccountRepository userRepository;
    private final PlatformAccountResponseMapper userResponseMapper;

    @Override
    public List<PlatformAccountSummaryResponseDTO> execute(Void unused) {
        UUID agencyId = TenantContext.requireAgencyId();
        return userRepository.findByAgency_IdOrderByNameAsc(agencyId).stream()
                .map(userResponseMapper::toResponse)
                .toList();
    }
}
