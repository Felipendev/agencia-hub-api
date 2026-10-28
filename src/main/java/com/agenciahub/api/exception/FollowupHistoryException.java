package com.agenciahub.api.exception;

public class FollowupHistoryException extends RuntimeException {
    public FollowupHistoryException(){super("Esta pessoa possui acompanhamento comercial ou histórico. Inative a pessoa quando precisar preservá-lo; excluir ou cancelar a ação não apaga o histórico.");}
}
