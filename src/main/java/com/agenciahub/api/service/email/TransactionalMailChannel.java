package com.agenciahub.api.service.email;

/** Transports a {@link TransactionalMail} — Resend, SMTP, log, etc. */
public interface TransactionalMailChannel {

    void send(String to, TransactionalMail mail);
}
