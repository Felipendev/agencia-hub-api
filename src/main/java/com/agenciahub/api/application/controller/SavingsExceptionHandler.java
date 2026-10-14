package com.agenciahub.api.application.controller;

import com.agenciahub.api.exception.ApiError;
import com.agenciahub.api.exception.SavingsConflictException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Erros do módulo de economia, sem alterar o handler global. */
@RestControllerAdvice(assignableTypes = {TripSavingsController.class, SavingsReportController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SavingsExceptionHandler {

    @ExceptionHandler(SavingsConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError conflict(SavingsConflictException ex) { return new ApiError(ex.getMessage(), "SAVINGS_CONFLICT"); }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError forbidden() { return new ApiError("Seu perfil não tem permissão para esta ação em economia.", "SAVINGS_ACCESS_DENIED"); }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError unreadable() {
        return new ApiError("Revise os campos da análise: valores, datas e opções devem estar em formato válido.", "INVALID_SAVINGS_INPUT");
    }
}
