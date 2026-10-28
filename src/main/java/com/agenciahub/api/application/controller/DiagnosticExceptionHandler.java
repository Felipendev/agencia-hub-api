package com.agenciahub.api.application.controller;
import com.agenciahub.api.exception.ApiError;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes={MilesDiagnosticController.class,PublicMilesDiagnosticController.class}) @Order(-20)
public class DiagnosticExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<ApiError> status(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(new ApiError(e.getReason(),"DIAGNOSTIC_ERROR"));}
    @ExceptionHandler({IllegalArgumentException.class,org.springframework.web.bind.MethodArgumentNotValidException.class,org.springframework.http.converter.HttpMessageNotReadableException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,org.springframework.web.bind.MissingRequestHeaderException.class})
    public ResponseEntity<ApiError> invalid(Exception e){return ResponseEntity.badRequest().body(new ApiError(e instanceof IllegalArgumentException?e.getMessage():"Revise os campos e o consentimento do diagnóstico.","INVALID_DIAGNOSTIC"));}
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class) public ResponseEntity<ApiError> forbidden(){return ResponseEntity.status(403).body(new ApiError("Seu perfil não permite esta operação no diagnóstico.","DIAGNOSTIC_ACCESS_DENIED"));}
}
