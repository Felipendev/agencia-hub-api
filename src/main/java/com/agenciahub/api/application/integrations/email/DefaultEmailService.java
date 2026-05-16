package com.agenciahub.api.application.integrations.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Builds copy via {@link TransactionalMailBody}, sends via {@link TransactionalMailChannel}
 * (wired in {@link com.agenciahub.api.config.MailDispatchConfiguration}).
 */
@Service
public class DefaultEmailService implements EmailService {

    private final TransactionalMailChannel channel;
    private final String appBaseUrl;

    public DefaultEmailService(
            TransactionalMailChannel channel,
            @Value("${app.base-url:http://localhost:3000}") String appBaseUrl) {
        this.channel = channel;
        this.appBaseUrl = appBaseUrl;
    }

    @Async
    @Override
    public void sendVerificationCode(String to, String code, String userName) {
        channel.send(to, TransactionalMailBody.verificationCode(userName, code, to, appBaseUrl));
    }

    @Async
    @Override
    public void sendInvitation(String to, String inviteUrl, String agencyName, String inviterName) {
        channel.send(to, TransactionalMailBody.invitation(inviteUrl, agencyName, inviterName));
    }

    @Async
    @Override
    public void sendPasswordResetCode(String to, String code, String userName) {
        channel.send(to, TransactionalMailBody.passwordReset(userName, code));
    }
}
