package com.agenciahub.api.application.integrations.email;

/** Transports a {@link TransactionalMail} — Resend, SMTP, log, etc. */
public interface TransactionalMailChannel {

    void send(String to, TransactionalMail mail);
}
