package com.agenciahub.api.domain.enums;

/**
 * Perfil de conta de plataforma (login) dentro de uma agência.
 * Não confundir com cliente CRM ({@code CrmCustomer}).
 */
public enum AccountKind {
    /** Dono da agência — acesso administrativo completo na agência. */
    AGENCY_OWNER,
    /** Agente de venda — convidado pela agência; cotações e painel restritos. */
    SALES_AGENT
}
