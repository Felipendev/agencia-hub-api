package com.agenciahub.api.application.integrations.email;

import com.agenciahub.api.security.VerificationLinkTokenService;
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
    private final VerificationLinkTokenService linkTokenService;
    private final String appBaseUrl;

    public DefaultEmailService(
            TransactionalMailChannel channel,
            VerificationLinkTokenService linkTokenService,
            @Value("${app.base-url:http://localhost:3000}") String appBaseUrl) {
        this.channel = channel;
        this.linkTokenService = linkTokenService;
        this.appBaseUrl = appBaseUrl;
    }

    @Async
    @Override
    public void sendVerificationCode(String to, String code, String userName) {
        String linkToken = linkTokenService.generate(to, code);
        channel.send(to, TransactionalMailBody.verificationCode(userName, code, to, appBaseUrl, linkToken));
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

    @Async
    @Override
    public void sendNewSubmissionAlert(String to, String agencyName, String clienteNome,
                                       String telefone, String rota, String datas, String dashboardUrl) {
        channel.send(to, TransactionalMailBody.newSubmissionAlert(
                agencyName, clienteNome, telefone, rota, datas, dashboardUrl, to));
    }
}
