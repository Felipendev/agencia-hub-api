package com.agenciahub.api.security;

import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.exception.UnauthenticatedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the authenticated user from {@link SecurityContextHolder} (JWT filter sets {@link PlatformAccount} as principal).
 */
public final class SecurityContextUsers {

    private SecurityContextUsers() {
    }

    /**
     * When the principal is the persisted {@link PlatformAccount} entity (typical for this API after JWT validation).
     */
    public static Optional<PlatformAccount> optionalUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return Optional.empty();
        }
        if (authentication.getPrincipal() instanceof PlatformAccount user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public static UUID requireUserId() {
        return optionalUser()
                .map(PlatformAccount::getId)
                .orElseGet(() -> {
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    if (authentication == null || authentication.getPrincipal() == null) {
                        throw new UnauthenticatedException("usuário não autenticado");
                    }
                    try {
                        return UUID.fromString(authentication.getName());
                    } catch (RuntimeException ex) {
                        throw new UnauthenticatedException("usuário não autenticado");
                    }
                });
    }

    public static PlatformAccount requireUser() {
        return optionalUser()
                .orElseThrow(() -> new UnauthenticatedException("usuário não autenticado"));
    }
}
