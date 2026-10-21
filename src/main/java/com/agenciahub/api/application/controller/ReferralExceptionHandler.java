package com.agenciahub.api.application.controller;

import com.agenciahub.api.exception.ApiError;
import com.agenciahub.api.exception.ReferralConflictException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Erros do módulo de indicações, sem alterar o handler global. */
@RestControllerAdvice(assignableTypes = ReferralController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReferralExceptionHandler {

    @ExceptionHandler(ReferralConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError conflict(ReferralConflictException ex) { return new ApiError(ex.getMessage(), "REFERRAL_CONFLICT"); }

    /** Corrida entre dois envios para a mesma pessoa e produto: o índice único parcial decide. */
    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError duplicate() {
        return new ApiError("Esta pessoa já tem uma indicação ativa neste produto. Abra a indicação existente ou cancele-a antes.", "REFERRAL_ACTIVE_EXISTS");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError forbidden() { return new ApiError("Seu perfil não tem acesso às indicações.", "REFERRAL_ACCESS_DENIED"); }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError unreadable() {
        return new ApiError("Revise os campos da indicação: produto, origem, situação e datas devem estar em formato válido.", "INVALID_REFERRAL_INPUT");
    }
}
