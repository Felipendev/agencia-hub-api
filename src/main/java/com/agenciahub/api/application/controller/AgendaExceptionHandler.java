package com.agenciahub.api.application.controller;

import com.agenciahub.api.exception.AgendaConflictException;
import com.agenciahub.api.exception.ApiError;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Erros do módulo de agenda, sem alterar o handler global. */
@RestControllerAdvice(assignableTypes = AgendaController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AgendaExceptionHandler {

    @ExceptionHandler(AgendaConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError conflict(AgendaConflictException ex) { return new ApiError(ex.getMessage(), "AGENDA_CONFLICT"); }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError forbidden() { return new ApiError("Seu perfil não tem acesso à agenda da agência.", "AGENDA_ACCESS_DENIED"); }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError unreadable() {
        return new ApiError("Revise os campos do compromisso: datas com fuso, duração em minutos e opções devem estar em formato válido.", "INVALID_AGENDA_INPUT");
    }
}
