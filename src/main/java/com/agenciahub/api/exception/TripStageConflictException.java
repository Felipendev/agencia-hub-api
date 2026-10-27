package com.agenciahub.api.exception;

/** Conflito de versão ou de estado em etapas da viagem (HTTP 409). */
public class TripStageConflictException extends RuntimeException {
    public TripStageConflictException(String message) { super(message); }
}
