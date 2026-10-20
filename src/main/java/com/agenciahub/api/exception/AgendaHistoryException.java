package com.agenciahub.api.exception;

public class AgendaHistoryException extends RuntimeException {
    public AgendaHistoryException() {
        super("Esta pessoa possui compromissos, atas ou histórico de agenda. Inative a pessoa para preservar esses registros; a exclusão exige revisão do histórico.");
    }
}
