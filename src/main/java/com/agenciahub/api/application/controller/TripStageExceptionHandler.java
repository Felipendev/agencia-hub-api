package com.agenciahub.api.application.controller;

import com.agenciahub.api.exception.ApiError;
import com.agenciahub.api.exception.TripStageConflictException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Erros do módulo de etapas, sem alterar o handler global. */
@RestControllerAdvice(assignableTypes = TripStageController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TripStageExceptionHandler {

    @ExceptionHandler(TripStageConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError conflict(TripStageConflictException ex) { return new ApiError(ex.getMessage(), "TRIP_STAGE_CONFLICT"); }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError forbidden() { return new ApiError("Seu perfil não tem acesso às etapas da viagem.", "TRIP_STAGE_ACCESS_DENIED"); }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError unreadable() {
        return new ApiError("Revise os campos da etapa: datas com fuso, número de pessoas e tipo devem estar em formato válido.", "INVALID_TRIP_STAGE_INPUT");
    }
}
