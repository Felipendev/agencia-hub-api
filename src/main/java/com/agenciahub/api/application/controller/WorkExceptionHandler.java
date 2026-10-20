package com.agenciahub.api.application.controller;

import com.agenciahub.api.exception.ApiError;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes=WorkController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WorkExceptionHandler {
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiError> forbidden(){
        return ResponseEntity.status(403).body(new ApiError("Seu perfil não tem permissão para gerenciar tarefas desta agência.","WORK_ACCESS_DENIED"));
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> workError(ResponseStatusException exception){
        return ResponseEntity.status(exception.getStatusCode()).body(new ApiError(exception.getReason(),"WORK_"+exception.getStatusCode().value()));
    }
}
