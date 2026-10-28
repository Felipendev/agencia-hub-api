package com.agenciahub.api.exception;
public class DiagnosticHistoryException extends RuntimeException {
    public DiagnosticHistoryException(){super("Esta pessoa possui diagnóstico de milhas incorporado. Revise as declarações e o histórico antes de excluir o cadastro.");}
}
