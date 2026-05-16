package com.agenciahub.api.application.usecases.platformaccount.create;

import com.agenciahub.api.application.usecases.platformaccount.shared.PlatformAccountResponseMapper;
import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.application.usecases.platformaccount.create.CreatePlatformAccountRequestDTO;
import com.agenciahub.api.application.usecases.platformaccount.shared.PlatformAccountSummaryResponseDTO;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.application.usecases.platformaccount.shared.PublicLinkCodeSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreatePlatformAccount implements CreatePlatformAccountUseCase {

    private final PlatformAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PublicLinkCodeSupport publicLinkCodeSupport;
    private final PlatformAccountResponseMapper userResponseMapper;

    @Override
    @Transactional
    public PlatformAccountSummaryResponseDTO execute(CreatePlatformAccountRequestDTO request) {
        if (request.accountKind() == AccountKind.SALES_AGENT) {
            throw new IllegalArgumentException(
                    "agente de venda deve ser criado via convite; use POST /invitations e o registo com token");
        }
        if (userRepository.existsByEmail(request.email().trim().toLowerCase())) {
            throw new IllegalArgumentException("este e-mail já está cadastrado");
        }
        PlatformAccount user = PlatformAccount.builder()
                .name(request.name().strip())
                .email(request.email().trim().toLowerCase())
                .publicLinkCode(publicLinkCodeSupport.allocate())
                .passwordHash(passwordEncoder.encode(request.password()))
                .accountKind(request.accountKind())
                .active(Boolean.TRUE)
                .commissionPct(request.commissionPct())
                .commissionFixed(request.commissionFixed())
                .build();
        return userResponseMapper.toResponse(userRepository.save(user));
    }
}
