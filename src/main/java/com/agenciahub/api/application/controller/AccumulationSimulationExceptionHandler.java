package com.agenciahub.api.application.controller;
import com.agenciahub.api.exception.ApiError;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes=AccumulationSimulationController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccumulationSimulationExceptionHandler {
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> forbidden() {
        return ResponseEntity.status(403).body(new ApiError("Seu perfil não tem permissão para acessar os planos de milhas desta agência.","SIMULATION_ACCESS_DENIED"));
    }
}
