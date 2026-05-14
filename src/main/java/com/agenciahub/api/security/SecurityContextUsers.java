package com.agenciahub.api.security;

import com.agenciahub.api.entity.User;
import com.agenciahub.api.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Resolves the authenticated user from {@link SecurityContextHolder} (JWT filter sets {@link User} as principal).
 */
public final class SecurityContextUsers {

    private SecurityContextUsers() {
    }

    public static UUID requireUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("usuário não autenticado");
        }
        if (authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (RuntimeException ex) {
            throw new IllegalStateException("usuário não autenticado");
        }
    }

    public static User requireUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResourceNotFoundException("usuário não encontrado");
        }
        if (authentication.getPrincipal() instanceof User user) {
            return user;
        }
        throw new ResourceNotFoundException("usuário não encontrado");
    }
}
