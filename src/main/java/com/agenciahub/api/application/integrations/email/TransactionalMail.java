package com.agenciahub.api.application.integrations.email;

import org.springframework.lang.Nullable;

/**
 * Transactional message with optional HTML body.
 * When {@code htmlBody} is present, channels that support HTML should prefer it;
 * {@code textBody} is always required as a plain-text fallback.
 */
public record TransactionalMail(String subject, String textBody, @Nullable String htmlBody) {

    /** Convenience constructor for plain-text-only messages. */
    public TransactionalMail(String subject, String textBody) {
        this(subject, textBody, null);
    }
}
