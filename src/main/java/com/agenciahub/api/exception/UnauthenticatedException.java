package com.agenciahub.api.exception;

/**
 * Raised when an endpoint requires an authenticated user id but the security context
 * has no usable principal (see {@link com.agenciahub.api.security.SecurityContextUsers#requireUserId()}).
 */
public class UnauthenticatedException extends IllegalStateException {

    public UnauthenticatedException(String message) {
        super(message);
    }
}
