package com.agenciahub.api.application.usecases.platformaccount.update;

import com.agenciahub.api.application.usecases.platformaccount.shared.PlatformAccountResponseMapper;
import com.agenciahub.api.application.usecases.platformaccount.update.UpdatePlatformAccountRequestDTO;
import com.agenciahub.api.application.usecases.platformaccount.shared.PlatformAccountSummaryResponseDTO;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdatePlatformAccount implements UpdatePlatformAccountUseCase {

    private final PlatformAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlatformAccountResponseMapper userResponseMapper;

    @Override
    public PlatformAccountSummaryResponseDTO execute(UpdatePlatformAccountCommand command) {
        java.util.UUID agencyId = TenantContext.requireAgencyId();
        PlatformAccount user = userRepository
                .findByIdAndAgency_Id(command.id(), agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("usuário não encontrado: " + command.id()));
        UpdatePlatformAccountRequestDTO request = command.request();

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
