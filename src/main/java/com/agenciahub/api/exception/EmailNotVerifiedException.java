package com.agenciahub.api.exception;

public class EmailNotVerifiedException extends RuntimeException {

    private final String email;

    public EmailNotVerifiedException(String email) {
        super("verifique seu e-mail para acessar o sistema");
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
