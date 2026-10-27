package com.agenciahub.api.exception;

public class ReferralHistoryException extends RuntimeException {
    public ReferralHistoryException() {
        super("Esta pessoa participa de indicações ou possui histórico preservado. Revise os vínculos e o histórico antes de excluir; inative a pessoa quando precisar preservá-los.");
    }
}
