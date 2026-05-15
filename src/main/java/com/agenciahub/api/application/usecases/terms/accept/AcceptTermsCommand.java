package com.agenciahub.api.application.usecases.terms.accept;

import java.util.UUID;

public record AcceptTermsCommand(UUID userId, String termsVersion) {}
