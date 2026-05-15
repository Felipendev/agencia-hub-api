package com.agenciahub.api.application.usecases.platformaccount.create;

import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.application.usecases.platformaccount.create.CreatePlatformAccountRequestDTO;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CreatePlatformAccountTest {

    @Mock
    private PlatformAccountRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private com.agenciahub.api.application.usecases.platformaccount.shared.PublicLinkCodeSupport publicLinkCodeSupport;

    @Mock
    private com.agenciahub.api.application.usecases.platformaccount.shared.PlatformAccountResponseMapper userResponseMapper;

    @InjectMocks
    private CreatePlatformAccount createUser;

    @Test
    void execute_whenRoleSeller_rejectsInviteOnlyFlow() {
        var request = new CreatePlatformAccountRequestDTO(
                "Vendedor",
                "vendedor@test.com",
                "password1",
                AccountKind.SALES_AGENT,
                null,
                null);

        assertThatThrownBy(() -> createUser.execute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("convite");

        verifyNoInteractions(userRepository);
    }
}
