package com.agenciahub.api.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String message, String code, String email) {

    public ApiError(String message, String code) {
        this(message, code, null);
    }
}
