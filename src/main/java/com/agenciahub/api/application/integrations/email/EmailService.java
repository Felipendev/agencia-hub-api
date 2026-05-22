package com.agenciahub.api.application.integrations.email;

/**
 * Transactional outbound email. Implementation: {@link DefaultEmailService}.
 * Transport is selected in {@link com.agenciahub.api.config.MailDispatchConfiguration} (Resend, SMTP, or log).
 */
public interface EmailService {

    void sendVerificationCode(String to, String code, String userName);

    void sendInvitation(String to, String inviteUrl, String agencyName, String inviterName);

    void sendPasswordResetCode(String to, String code, String userName);

    void sendNewSubmissionAlert(String to, String agencyName, String clienteNome,
                                String telefone, String rota, String datas, String dashboardUrl);

    void sendDataDeletionRequestConfirmation(String to);

    void sendDataDeletionOwnerNotification(String to, String requestId);

    void sendDataDeletionProcessed(String to, boolean accepted, String justificativa);
}
