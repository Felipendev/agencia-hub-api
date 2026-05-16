package com.agenciahub.api.application.usecases.auth.shared;

import java.util.Set;

/** E-mails autorizados ao registo de agência durante fase beta (lista fechada). */
public final class AuthBetaWhitelist {

    public static final Set<String> ALLOWED_OWNER_EMAILS = Set.of(
            "contato@agenciashub.com.br",
            "felipehenrique.pds@gmail.com",
            "fhps.dev@gmail.com",
            "consultoria.andressaviagens@gmail.com");

    private AuthBetaWhitelist() {}
}
