package com.agenciahub.api.exception;

/**
 * Lançada quando se tenta criar ou atualizar um cliente com e-mail ou telefone
 * já cadastrado por outro registro.
 */
public class DuplicateCustomerException extends RuntimeException {

    public DuplicateCustomerException(String field, String value) {
        super("já existe um cliente cadastrado com " + field + " \"" + value + "\"");
    }
}
