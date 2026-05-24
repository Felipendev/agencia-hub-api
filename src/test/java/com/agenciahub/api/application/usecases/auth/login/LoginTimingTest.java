package com.agenciahub.api.application.usecases.auth.login;

import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.application.usecases.platformaccount.shared.PublicLinkCodeSupport;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginTimingTest {

    @Mock private PlatformAccountRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private PublicLinkCodeSupport publicLinkCodeSupport;

    @InjectMocks
    private Login login;

    @Test
    void execute_whenEmailNotFound_passwordEncoderIsStillCalled() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> login.execute(new LoginRequestDTO("unknown@test.com", "anypass")))
                .isInstanceOf(ResourceNotFoundException.class);

        // BCrypt must be called even when the email does not exist — prevents timing attack
        verify(passwordEncoder).matches(anyString(), anyString());
    }

    @Test
    void execute_whenEmailNotFound_takesAtLeastMinimumResponseTime() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        long start = System.currentTimeMillis();
        assertThatThrownBy(() -> login.execute(new LoginRequestDTO("unknown@test.com", "anypass")))
                .isInstanceOf(ResourceNotFoundException.class);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed).isGreaterThanOrEqualTo(200);
    }

    @Test
    void execute_whenWrongPassword_passwordEncoderIsCalledWithRealHash() {
        Agency agency = Agency.builder().build();
        PlatformAccount user = PlatformAccount.builder()
                .email("user@test.com")
                .passwordHash("real-bcrypt-hash")
                .active(Boolean.TRUE)
                .emailVerified(Boolean.TRUE)
                .agency(agency)
                .build();
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> login.execute(new LoginRequestDTO("user@test.com", "wrongpass")))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(passwordEncoder).matches("wrongpass", "real-bcrypt-hash");
    }
}
