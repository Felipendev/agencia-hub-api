package com.agenciahub.api.application.usecases.quotation.retrieve.list;

import com.agenciahub.api.domain.QuotationStatus;
import com.agenciahub.api.entity.User;

import java.util.UUID;

/**
 * Parâmetros de listagem de cotações já no contexto HTTP (usuário autenticado opcional).
 */
public record ListQuotationsQuery(
        UUID customerId,
        QuotationStatus status,
        String search,
        User caller
) {
}
