package com.agenciahub.api.exception;

import com.agenciahub.api.domain.contract.Contracts.FieldView;
import java.util.List;

/** Geração recusada porque o modelo usa campos sem valor (HTTP 422). */
public class ContractIncompleteException extends RuntimeException {
    private final List<FieldView> missingFields;

    public ContractIncompleteException(List<FieldView> missingFields) {
        super("Preencha os campos destacados antes de gerar o documento.");
        this.missingFields = List.copyOf(missingFields);
    }

    public List<FieldView> missingFields() { return missingFields; }
}
