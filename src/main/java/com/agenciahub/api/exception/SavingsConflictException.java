package com.agenciahub.api.exception;

/** Conflito de versão em análises de economia (HTTP 409). */
public class SavingsConflictException extends RuntimeException {
    public SavingsConflictException(String message) { super(message); }
}
