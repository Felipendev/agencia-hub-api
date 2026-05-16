package com.agenciahub.api.exception;

/**
 * Raised when an authenticated owner flow expects {@link com.agenciahub.api.security.TenantContext}
 * but no agency id was bound for the request (JWT / interceptor ordering).
 */
public class MissingAgencyContextException extends IllegalStateException {

    public MissingAgencyContextException(String message) {
        super(message);
    }
}
