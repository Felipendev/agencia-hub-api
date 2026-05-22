package com.agenciahub.api.web;

import com.agenciahub.api.exception.AccountDeletionPendingException;
import com.agenciahub.api.exception.ApiError;
import com.agenciahub.api.exception.DetalhesValidationException;
import com.agenciahub.api.exception.DuplicateCustomerException;
import com.agenciahub.api.exception.EmailNotVerifiedException;
import com.agenciahub.api.exception.MissingAgencyContextException;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.exception.UnauthenticatedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountDeletionPendingException.class)
    public ResponseEntity<ApiError> handleAccountDeletionPending(AccountDeletionPendingException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError(ex.getMessage(), "ACCOUNT_DELETION_PENDING"));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiError(ex.getMessage(), "NOT_FOUND"));
    }

    @ExceptionHandler(DuplicateCustomerException.class)
    public ResponseEntity<ApiError> handleDuplicateCustomer(DuplicateCustomerException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiError(ex.getMessage(), "DUPLICATE_CUSTOMER"));
    }

    /**
     * Captura violações de constraint único do banco (fallback caso a verificação
     * no service não tenha pego a race condition).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        String msg = ex.getMostSpecificCause().getMessage();
        if (msg != null && msg.contains("idx_customers_email_unique")) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new ApiError("já existe um cliente cadastrado com este e-mail", "DUPLICATE_CUSTOMER"));
        }
        if (msg != null && msg.contains("idx_customers_phone_unique")) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new ApiError("já existe um cliente cadastrado com este telefone", "DUPLICATE_CUSTOMER"));
        }
        log.error("Data integrity violation", ex);
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiError("violação de integridade de dados.", "DATA_INTEGRITY"));
    }

    @ExceptionHandler(DetalhesValidationException.class)
    public ResponseEntity<ApiError> handleDetalhesValidation(DetalhesValidationException ex) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ApiError(ex.getMessage(), "DETALHES_VALIDATION_ERROR"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(ex.getMessage(), "BAD_REQUEST"));
    }

    @ExceptionHandler(MissingAgencyContextException.class)
    public ResponseEntity<ApiError> handleMissingAgencyContext(MissingAgencyContextException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ApiError(ex.getMessage(), "MISSING_AGENCY_CONTEXT"));
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<ApiError> handleUnauthenticated(UnauthenticatedException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError(ex.getMessage(), "UNAUTHENTICATED"));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ApiError> handleEmailNotVerified(EmailNotVerifiedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ApiError(ex.getMessage(), "EMAIL_NOT_VERIFIED", ex.getEmail()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(ex.getMessage(), "BAD_REQUEST"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> "campo '" + fe.getField() + "' " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(detail, "VALIDATION_ERROR"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        log.error("Unhandled error", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("erro interno do servidor", "INTERNAL_ERROR"));
    }
}
