package com.agenciahub.api.service.email;

/** Plain-text transactional message (subject + body). */
public record TransactionalMail(String subject, String textBody) {}
