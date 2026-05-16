package com.agenciahub.api.security;

import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.exception.UnauthenticatedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityContextUsersTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void optionalUser_isEmpty_whenNoAuthentication() {
        assertEquals(Optional.empty(), SecurityContextUsers.optionalUser());
    }

    @Test
    void optionalUser_isEmpty_whenPrincipalIsNotUser() {
        setAuthentication(new UsernamePasswordAuthenticationToken("anonymous", null, List.of()));
        assertEquals(Optional.empty(), SecurityContextUsers.optionalUser());
    }

    @Test
    void optionalUser_returnsUser_whenPrincipalIsUserEntity() {
        PlatformAccount user = sampleUser();
        setAuthentication(new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_AGENCY_OWNER"))));

        assertTrue(SecurityContextUsers.optionalUser().isPresent());
        assertEquals(user.getId(), SecurityContextUsers.optionalUser().orElseThrow().getId());
    }

    @Test
    void requireUserId_returnsUserId_whenPrincipalIsUser() {
        PlatformAccount user = sampleUser();
        setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));

        assertEquals(user.getId(), SecurityContextUsers.requireUserId());
    }

    @Test
    void requireUser_returnsSameInstance_whenPrincipalIsUser() {
        PlatformAccount user = sampleUser();
        setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));

        assertEquals(user, SecurityContextUsers.requireUser());
    }

    @Test
    void requireUser_throwsUnauthenticated_whenNoAuthentication() {
        assertThrows(UnauthenticatedException.class, SecurityContextUsers::requireUser);
    }

    @Test
    void requireUser_throwsUnauthenticated_whenPrincipalIsNotUser() {
        setAuthentication(new UsernamePasswordAuthenticationToken(42, null, List.of()));
        assertThrows(UnauthenticatedException.class, SecurityContextUsers::requireUser);
    }

    @Test
    void requireUserId_throwsUnauthenticated_whenNoAuthentication() {
        assertThrows(UnauthenticatedException.class, SecurityContextUsers::requireUserId);
    }

    @Test
    void requireUserId_throwsUnauthenticated_whenPrincipalIsNotUserAndNameIsNotUuid() {
        setAuthentication(new UsernamePasswordAuthenticationToken(42, null, List.of()));
        UnauthenticatedException ex = assertThrows(UnauthenticatedException.class, SecurityContextUsers::requireUserId);
        assertEquals("usuário não autenticado", ex.getMessage());
    }

    @Test
    void requireUserId_parsesAuthenticationName_whenPrincipalIsNotUserButNameIsUuid() {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        var auth = new UsernamePasswordAuthenticationToken(42, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        // Spring's getName() uses principal.toString() when principal is not UserDetails
        assertFalse(SecurityContextUsers.optionalUser().isPresent());
        // Cannot rely on getName() for Integer principal being UUID — use string principal:
        setAuthentication(new UsernamePasswordAuthenticationToken(
                "legacy", null, List.of()) {
            @Override
            public String getName() {
                return id.toString();
            }
        });
        assertEquals(id, SecurityContextUsers.requireUserId());
    }

    private static PlatformAccount sampleUser() {
        return PlatformAccount.builder()
                .id(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .name("Tester")
                .email("tester@example.com")
                .passwordHash("hash")
                .accountKind(AccountKind.AGENCY_OWNER)
                .build();
    }

    private static void setAuthentication(UsernamePasswordAuthenticationToken authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
