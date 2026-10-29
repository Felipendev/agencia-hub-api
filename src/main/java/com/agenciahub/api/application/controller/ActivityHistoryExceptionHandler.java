package com.agenciahub.api.application.controller;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import java.util.Map;

@RestControllerAdvice(assignableTypes=ActivityHistoryController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ActivityHistoryExceptionHandler {
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<?> denied(org.springframework.security.access.AccessDeniedException e) {
        return ResponseEntity.status(403).body(Map.of("code","HISTORY_FORBIDDEN","message","Seu perfil não permite esta operação no histórico."));
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("code","HISTORY_FILTER","message",e.getReason()==null?"Não foi possível consultar o histórico.":e.getReason()));
    }
    @ExceptionHandler({MethodArgumentTypeMismatchException.class,MissingServletRequestParameterException.class})
    public ResponseEntity<?> filter(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("code","HISTORY_FILTER","message","Revise produto, período, módulos e pessoa selecionada."));
    }
}
