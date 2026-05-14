package com.agenciahub.api.security;

import com.agenciahub.api.entity.User;
import com.agenciahub.api.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the authenticated user from {@link SecurityContextHolder} (JWT filter sets {@link User} as principal).
 */
public final class SecurityContextUsers {

    private SecurityContextUsers() {
    }

    /**
     * When the principal is the persisted {@link User} entity (typical for this API after JWT validation).
     */
    public static Optional<User> optionalUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return Optional.empty();
        }
        if (authentication.getPrincipal() instanceof User user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public static UUID requireUserId() {
        return optionalUser()
                .map(User::getId)
                .orElseGet(() -> {
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    if (authentication == null || authentication.getPrincipal() == null) {
                        throw new IllegalStateException("usuário não autenticado");
                    }
                    try {
                        return UUID.fromString(authentication.getName());
                    } catch (RuntimeException ex) {
                        throw new IllegalStateException("usuário não autenticado");
                    }
                });
    }

    public static User requireUser() {
        return optionalUser()
                .orElseThrow(() -> new ResourceNotFoundException("usuário não encontrado"));
    }
}
