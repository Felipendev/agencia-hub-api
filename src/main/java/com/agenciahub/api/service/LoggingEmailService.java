package com.agenciahub.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * No-op fallback implementation of {@link EmailService} that logs email content
 * instead of sending it. Activated when no SMTP configuration is present.
 */
@Service
@ConditionalOnMissingBean(SmtpEmailService.class)
public class LoggingEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailService.class);

    @Override
    public void sendVerificationCode(String to, String code, String userName) {
        log.info("[EMAIL-NOOP] Verification code for {} ({}): {}", userName, to, code);
    }

    @Override
    public void sendInvitation(String to, String inviteUrl, String agencyName, String inviterName) {
        log.info("[EMAIL-NOOP] Invitation to {} for agency '{}' from {}: {}", to, agencyName, inviterName, inviteUrl);
    }

    @Override
    public void sendPasswordResetCode(String to, String code, String userName) {
        log.info("[EMAIL-NOOP] Password reset code for {} ({}): {}", userName, to, code);
    }
}
