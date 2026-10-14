package com.agenciahub.api.application.controller;

import com.agenciahub.api.domain.contract.Contracts.FieldView;
import com.agenciahub.api.exception.ApiError;
import com.agenciahub.api.exception.ContractConflictException;
import com.agenciahub.api.exception.ContractIncompleteException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/** Respostas de erro do módulo de contratos, sem alterar o handler global. */
@RestControllerAdvice(assignableTypes = {ContractController.class, ContractTemplateController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ContractExceptionHandler {

    public record IncompleteError(String message, String code, List<FieldView> missingFields) {}

    @ExceptionHandler(ContractConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError conflict(ContractConflictException ex) { return new ApiError(ex.getMessage(), "CONTRACT_CONFLICT"); }

    @ExceptionHandler(ContractIncompleteException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public IncompleteError incomplete(ContractIncompleteException ex) {
        return new IncompleteError(ex.getMessage(), "CONTRACT_INCOMPLETE", ex.missingFields());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError forbidden(AccessDeniedException ex) {
        String message = ex.getMessage() != null && ex.getMessage().startsWith("Somente")
                ? ex.getMessage() : "Seu perfil não tem permissão para esta ação em contratos.";
        return new ApiError(message, "CONTRACT_ACCESS_DENIED");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError unreadable() {
        return new ApiError("Revise os campos do contrato: valores, datas e opções devem estar em formato válido.", "INVALID_CONTRACT_INPUT");
    }
}
