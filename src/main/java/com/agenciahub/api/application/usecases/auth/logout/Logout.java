package com.agenciahub.api.application.usecases.auth.logout;

import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class Logout {

    private final PlatformAccountRepository repository;

    @Transactional
    public void execute(UUID userId) {
        var user = repository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("usuário não encontrado"));
        user.setLastLogoutAt(Instant.now());
        repository.save(user);
    }
}
