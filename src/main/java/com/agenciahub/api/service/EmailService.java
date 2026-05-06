package com.agenciahub.api.service;

/**
 * Abstraction for sending transactional emails.
 * Implementations may use SMTP, SES, SendGrid, or simply log (for dev/test).
 */
public interface EmailService {

    /**
     * Sends a 6-digit verification code to the user.
     *
     * @param to       recipient email address
     * @param code     the 6-digit verification code (plain text)
     * @param userName the user's display name for personalization
     */
    void sendVerificationCode(String to, String code, String userName);

    /**
     * Sends an invitation email with a registration link.
     *
     * @param to          recipient email address
     * @param inviteUrl   full URL for the invitation registration page
     * @param agencyName  name of the inviting agency
     * @param inviterName name of the person who sent the invitation
     */
    void sendInvitation(String to, String inviteUrl, String agencyName, String inviterName);

    /**
     * Sends a password reset code to the user.
     *
     * @param to       recipient email address
     * @param code     the 6-digit reset code (plain text)
     * @param userName the user's display name for personalization
     */
    void sendPasswordResetCode(String to, String code, String userName);
}
