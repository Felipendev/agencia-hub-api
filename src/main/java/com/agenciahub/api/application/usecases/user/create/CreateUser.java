package com.agenciahub.api.application.usecases.user.create;

import com.agenciahub.api.application.usecases.user.shared.UserResponseMapper;
import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.application.usecases.user.create.CreateUserRequestDTO;
import com.agenciahub.api.application.usecases.user.shared.UserSummaryResponseDTO;
import com.agenciahub.api.entity.PlatformAccount;
import com.agenciahub.api.repository.PlatformAccountRepository;
import com.agenciahub.api.application.usecases.user.shared.PublicLinkCodeSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateUser implements CreateUserUseCase {

    private final PlatformAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PublicLinkCodeSupport publicLinkCodeSupport;
    private final UserResponseMapper userResponseMapper;

    @Override
    @Transactional
    public UserSummaryResponseDTO execute(CreateUserRequestDTO request) {
        if (request.role() == AccountKind.SALES_AGENT) {
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
                .role(request.role())
                .active(Boolean.TRUE)
                .commissionPct(request.commissionPct())
                .commissionFixed(request.commissionFixed())
                .build();
        return userResponseMapper.toResponse(userRepository.save(user));
    }
}
