package com.agenciahub.api.exception;

/** Conflito de versão ou de estado em compromissos e atas (HTTP 409). */
public class AgendaConflictException extends RuntimeException {
    public AgendaConflictException(String message) { super(message); }
}
