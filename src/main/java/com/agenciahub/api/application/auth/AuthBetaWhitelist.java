package com.agenciahub.api.application.auth;

import java.util.Set;

/** E-mails autorizados ao registo de agência durante fase beta (lista fechada). */
public final class AuthBetaWhitelist {

    public static final Set<String> ALLOWED_OWNER_EMAILS = Set.of(
            "contato@agenciashub.com.br",
            "consultoria.andressaviagens@gmail.com");

    private AuthBetaWhitelist() {}
}
