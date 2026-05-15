package com.agenciahub.api.domain.user;

import com.agenciahub.api.domain.enums.AccountKind;

/**
 * Perfil de membro da agência com login (dono ou agente de venda).
 * Evolução futura: tipos dedicados {@code AgencyOwnerProfile}, {@code SalesAgentProfile}.
 */
public interface AgencyMemberProfile {

    AccountKind kind();
}
