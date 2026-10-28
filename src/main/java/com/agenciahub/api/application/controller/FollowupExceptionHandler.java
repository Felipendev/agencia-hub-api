package com.agenciahub.api.application.controller;

import com.agenciahub.api.exception.ApiError;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes=FollowupController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FollowupExceptionHandler {
    @ExceptionHandler({org.springframework.web.bind.ServletRequestBindingException.class,
            org.springframework.beans.TypeMismatchException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class,
            jakarta.validation.ConstraintViolationException.class})
    public ResponseEntity<ApiError> invalidInput(){return ResponseEntity.badRequest().body(new ApiError("Revise o produto, os filtros, as datas e os identificadores informados.","FOLLOWUP_INVALID_INPUT"));}
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> error(ResponseStatusException ex){return ResponseEntity.status(ex.getStatusCode()).body(new ApiError(ex.getReason(),"FOLLOWUP_"+ex.getStatusCode().value()));}
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiError> denied(){return ResponseEntity.status(403).body(new ApiError("Seu perfil não pode acessar o acompanhamento comercial.","FOLLOWUP_ACCESS_DENIED"));}
}
