package com.agenciahub.api.security;

import com.agenciahub.api.entity.User;
import com.agenciahub.api.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        if (!jwtService.isValid(token)) {
            chain.doFilter(request, response);
            return;
        }

        UUID userId = jwtService.extractUserId(token);
        User user = userRepository.findByIdWithAgency(userId).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getActive())) {
            chain.doFilter(request, response);
            return;
        }

        // Validate password_changed_at: reject tokens issued before a password change
        Long tokenPasswordChangedAt = jwtService.extractPasswordChangedAt(token);
        if (user.getPasswordChangedAt() != null) {
            long userPasswordChangedEpoch = user.getPasswordChangedAt().getEpochSecond();
            if (tokenPasswordChangedAt == null || tokenPasswordChangedAt < userPasswordChangedEpoch) {
                // Token was issued before the password was changed — reject it
                chain.doFilter(request, response);
                return;
            }
        }

        // Extract agency_id from token and set TenantContext
        UUID agencyId = jwtService.extractAgencyId(token);
        if (agencyId != null) {
            TenantContext.set(agencyId);
        } else if (user.getAgency() != null) {
            // Fallback: use agency from user entity (for legacy tokens without agency_id claim)
            TenantContext.set(user.getAgency().getId());
        }

        var auth = new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);

        chain.doFilter(request, response);
    }
}
