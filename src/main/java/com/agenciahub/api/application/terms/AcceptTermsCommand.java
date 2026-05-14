package com.agenciahub.api.application.terms;

import java.util.UUID;

public record AcceptTermsCommand(UUID userId, String termsVersion) {
}
