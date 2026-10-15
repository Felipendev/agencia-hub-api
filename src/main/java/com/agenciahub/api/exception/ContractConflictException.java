package com.agenciahub.api.exception;

/** Conflito de versão ou de estado em contratos/modelos (HTTP 409). */
public class ContractConflictException extends RuntimeException {
    public ContractConflictException(String message) { super(message); }
}
