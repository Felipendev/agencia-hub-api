package com.agenciahub.api.application.controller;
import com.agenciahub.api.exception.ApiError;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes=SimulationTaskController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SimulationTaskExceptionHandler {
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> forbidden(){return ResponseEntity.status(403).body(new ApiError("Seu perfil não pode gerar tarefas de Milhas.","PLAN_TASK_ACCESS_DENIED"));}
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> error(ResponseStatusException ex){return ResponseEntity.status(ex.getStatusCode()).body(new ApiError(ex.getReason(),"PLAN_TASK_"+ex.getStatusCode().value()));}
    @ExceptionHandler({org.springframework.web.bind.ServletRequestBindingException.class,org.springframework.beans.TypeMismatchException.class,org.springframework.http.converter.HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> invalid(Exception ex){return ResponseEntity.badRequest().body(new ApiError("Revise os dados da geração.","PLAN_TASK_INVALID"));}
}
