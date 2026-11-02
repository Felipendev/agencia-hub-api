package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.application.persistence.repository.AgencyRepository;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.application.usecases.auth.login.LoginResponseDTO;
import com.agenciahub.api.application.usecases.platformaccount.shared.PublicLinkCodeSupport;
import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.domain.SubscriptionStatus;
import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

/** Dedicated sandbox identity: arbitrary credentials never impersonate a real account. */
@RestController
@Profile("local-login & !prod & !production")
public class LocalLoginController {
    private final PlatformAccountRepository accounts;
    private final AgencyRepository agencies;
    private final PasswordEncoder passwords;
    private final JwtService jwt;
    private final PublicLinkCodeSupport links;

    public LocalLoginController(PlatformAccountRepository accounts, AgencyRepository agencies,
            PasswordEncoder passwords, JwtService jwt, PublicLinkCodeSupport links, Environment env) {
        if (!"127.0.0.1".equals(env.getProperty("server.address"))
                || !env.getProperty("spring.datasource.url", "").matches("jdbc:postgresql://(?:localhost|127\\.0\\.0\\.1):[0-9]+/[^?]+")) {
            throw new IllegalStateException("Login livre exige servidor e PostgreSQL locais.");
        }
        this.accounts = accounts; this.agencies = agencies; this.passwords = passwords; this.jwt = jwt; this.links = links;
    }

    @PostMapping("/internal/local-login")
    @Transactional
    public synchronized LoginResponseDTO login(HttpServletRequest request) {
        if (!"127.0.0.1".equals(request.getRemoteAddr()) && !"0:0:0:0:0:0:0:1".equals(request.getRemoteAddr()) && !"::1".equals(request.getRemoteAddr())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        PlatformAccount user = accounts.findByEmail("sandbox@agencia-hub.invalid").orElseGet(() -> {
            Agency agency = agencies.save(Agency.builder().name("Agência de testes local")
                    .status(AgencyStatus.ACTIVE).subscriptionStatus(SubscriptionStatus.ACTIVE).build());
            return accounts.saveAndFlush(PlatformAccount.builder().agency(agency).name("Dono — teste local")
                    .email("sandbox@agencia-hub.invalid").passwordHash(passwords.encode(UUID.randomUUID().toString()))
                    .accountKind(AccountKind.AGENCY_OWNER).active(true).emailVerified(true).termsAccepted(true)
                    .mustChangePassword(false).notifEmailSubmissao(false).notifEmailCotacaoAprovada(false)
                    .notifEmailCotacaoVencendo(false).notifEmailCotacaoVencida(false).notifEmailExclusaoAgendada(false).build());
        });
        Agency agency = user.getAgency();
        if (user.getPublicLinkCode() == null || user.getPublicLinkCode().isBlank()) {
            user.setPublicLinkCode(links.allocate());
            accounts.save(user);
        }
        String code = user.getPublicLinkCode();
        return new LoginResponseDTO(jwt.generate(user.getId(), user.getAccountKind().name(), agency.getId(), user.getPasswordChangedAt()),
                user.getId(), user.getName(), user.getEmail(), user.getAccountKind(), agency.getId(), agency.getName(),
                agency.getStatus(), agency.getSubscriptionStatus(), agency.getTrialEndsAt(), false, code, false);
    }
}
