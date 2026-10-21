package com.agenciahub.api.exception;

public class TripStageHistoryException extends RuntimeException {
    public TripStageHistoryException() {
        super("Esta pessoa possui etapas de viagem ou histórico preservado. Revise os dados e o histórico antes de excluir a pessoa.");
    }
}
