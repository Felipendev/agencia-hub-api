package com.agenciahub.api.application.invitation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InvitationLinkBuilder {

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    public String buildInviteUrl(String token) {
        return baseUrl + "/convite/" + token;
    }
}
