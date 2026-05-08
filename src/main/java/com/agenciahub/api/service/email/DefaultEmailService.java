package com.agenciahub.api.service.email;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.agenciahub.api.service.EmailService;

/**
 * Single {@link EmailService} entry point: builds copy via {@link TransactionalMailBody},
 * sends via {@link TransactionalMailChannel} (wired in {@link com.agenciahub.api.config.MailDispatchConfiguration}).
 */
@Service
public class DefaultEmailService implements EmailService {

    private final TransactionalMailChannel channel;

    public DefaultEmailService(TransactionalMailChannel channel) {
        this.channel = channel;
    }

    @Async
    @Override
    public void sendVerificationCode(String to, String code, String userName) {
        channel.send(to, TransactionalMailBody.verificationCode(userName, code));
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
