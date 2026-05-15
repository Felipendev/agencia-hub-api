package com.agenciahub.api.application.integrations.email;

/** Plain-text transactional message (subject + body). */
public record TransactionalMail(String subject, String textBody) {}
