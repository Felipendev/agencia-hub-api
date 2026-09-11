package com.agenciahub.api.application.controller;

import com.agenciahub.api.security.JwtService;
import com.agenciahub.api.security.SecurityContextUsers;
import com.agenciahub.api.domain.AgencyStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SessionController {
    private final JwtService jwt;

    @PostMapping("/auth/session/renew")
    public ResponseEntity<Map<String, String>> renew(@RequestHeader("Authorization") String authorization) {
        var user = SecurityContextUsers.requireUser();
        if (user.getAgency() != null) {
            // Same gate as login (Login.java): TRIAL/ACTIVE/SUSPENDED may keep working, only these block access.
            AgencyStatus status = user.getAgency().getStatus();
            if (status == AgencyStatus.DELETION_PENDING || status == AgencyStatus.PENDING_VERIFICATION
                    || status == AgencyStatus.CANCELED) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Agência indisponível");
            }
        }
        try {
            String token = jwt.renew(authorization.substring(7), user.getId(), user.getAccountKind().name(),
                    user.getAgency() == null ? null : user.getAgency().getId(), user.getPasswordChangedAt());
            return ResponseEntity.ok().header("Cache-Control", "no-store").body(Map.of("token", token));
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão encerrada");
        }
    }
}
