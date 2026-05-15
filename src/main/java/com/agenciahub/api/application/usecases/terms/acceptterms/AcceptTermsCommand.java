package com.agenciahub.api.application.usecases.terms.acceptterms;

import java.util.UUID;

public record AcceptTermsCommand(UUID userId, String termsVersion) {}
