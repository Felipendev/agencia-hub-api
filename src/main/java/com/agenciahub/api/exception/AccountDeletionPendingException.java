package com.agenciahub.api.exception;

public class AccountDeletionPendingException extends RuntimeException {
    public AccountDeletionPendingException() {
        super("esta conta está em processo de exclusão. entre em contato com o suporte caso precise de ajuda.");
    }
}
