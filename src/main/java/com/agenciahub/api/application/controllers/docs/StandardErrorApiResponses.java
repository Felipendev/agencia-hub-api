package com.agenciahub.api.application.controllers.docs;

import com.agenciahub.api.exception.ApiError;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Respostas de erro padrão ({@link ApiError}) para documentação OpenAPI em interfaces {@code *API}.
 * Pode ser usada em tipo ou em método; combine com {@code @Operation} nos endpoints.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ApiResponses({
        @ApiResponse(
                responseCode = "400",
                description = "Requisição inválida, validação Bean Validation ou estado ilegal (`BAD_REQUEST` ou `VALIDATION_ERROR`).",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Não autenticado (`UNAUTHENTICATED`).",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Tenant de agência ausente no request (`MISSING_AGENCY_CONTEXT`).",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Recurso inexistente (`NOT_FOUND`).",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Conflito (`DUPLICATE_CUSTOMER` ou `DATA_INTEGRITY`).",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "500",
                description = "Erro interno não tratado (`INTERNAL_ERROR`).",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
})
public @interface StandardErrorApiResponses {
}
