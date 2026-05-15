package com.agenciahub.api.application.usecases.user.create;

import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.application.usecases.user.create.CreateUserRequestDTO;
import com.agenciahub.api.repository.PlatformAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CreateUserTest {

    @Mock
    private PlatformAccountRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private com.agenciahub.api.application.usecases.user.shared.PublicLinkCodeSupport publicLinkCodeSupport;

    @Mock
    private com.agenciahub.api.application.usecases.user.shared.UserResponseMapper userResponseMapper;

    @InjectMocks
    private CreateUser createUser;

    @Test
    void execute_whenRoleSeller_rejectsInviteOnlyFlow() {
        var request = new CreateUserRequestDTO(
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
