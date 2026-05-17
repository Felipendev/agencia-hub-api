package com.agenciahub.api.application.usecases.salesagent.retrieve.listactive;

import com.agenciahub.api.application.usecases.platformaccount.shared.PlatformAccountResponseMapper;
import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.application.usecases.platformaccount.shared.PlatformAccountSummaryResponseDTO;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListActiveSalesAgents implements ListActiveSalesAgentsUseCase {

    private final PlatformAccountRepository userRepository;
    private final PlatformAccountResponseMapper userResponseMapper;

    @Override
    public List<PlatformAccountSummaryResponseDTO> execute(Void unused) {
        var agencyId = TenantContext.requireAgencyId();
        return userRepository
                .findByAgency_IdAndAccountKindAndActiveTrue(agencyId, AccountKind.SALES_AGENT)
                .stream()
                .map(userResponseMapper::toResponse)
                .toList();
    }
}
