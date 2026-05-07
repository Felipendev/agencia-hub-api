package com.agenciahub.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * SMTP-based implementation of {@link EmailService}.
 * Activated when spring.mail.host is a non-empty string.
 * Sends are fire-and-forget: errors are logged but not propagated.
 */
@Service
@ConditionalOnExpression("!'${spring.mail.host:}'.isEmpty()")
public class SmtpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpEmailService(JavaMailSender mailSender,
                            @Value("${email.from:contato@agenciashub.com.br}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Async
    @Override
    public void sendVerificationCode(String to, String code, String userName) {
        String subject = "AgênciasHub — Código de Verificação";
        String body = String.format(
                "Olá %s,\n\nSeu código de verificação é: %s\n\nEste código expira em 15 minutos.\n\nSe você não solicitou este código, ignore este e-mail.\n\nEquipe AgênciasHub",
                userName, code);
        send(to, subject, body);
    }

    @Async
    @Override
    public void sendInvitation(String to, String inviteUrl, String agencyName, String inviterName) {
        String subject = String.format("AgênciasHub — Convite para %s", agencyName);
        String body = String.format(
                "Olá,\n\n%s convidou você para fazer parte da agência %s no AgênciasHub.\n\nPara aceitar o convite e criar sua conta, acesse o link abaixo:\n%s\n\nEste convite expira em 72 horas.\n\nEquipe AgênciasHub",
                inviterName, agencyName, inviteUrl);
        send(to, subject, body);
    }

    @Async
    @Override
    public void sendPasswordResetCode(String to, String code, String userName) {
        String subject = "AgênciasHub — Redefinição de Senha";
        String body = String.format(
                "Olá %s,\n\nSeu código para redefinição de senha é: %s\n\nEste código expira em 15 minutos.\n\nSe você não solicitou a redefinição, ignore este e-mail.\n\nEquipe AgênciasHub",
                userName, code);
        send(to, subject, body);
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.debug("Email sent to {} with subject '{}'", to, subject);
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }
}
