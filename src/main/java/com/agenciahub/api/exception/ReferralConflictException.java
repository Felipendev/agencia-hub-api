package com.agenciahub.api.exception;

/** Conflito de versão, de estado ou de indicação ativa duplicada (HTTP 409). */
public class ReferralConflictException extends RuntimeException {
    public ReferralConflictException(String message) { super(message); }
}
