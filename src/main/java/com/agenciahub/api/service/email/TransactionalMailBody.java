package com.agenciahub.api.service.email;

/** Single place for Portuguese copy — add locales or templates here later. */
public final class TransactionalMailBody {

    private TransactionalMailBody() {}

    public static TransactionalMail verificationCode(String userName, String code) {
        String body = """
                Olá %s,

                Seu código de verificação é: %s

                Este código expira em 15 minutos.

                Se você não solicitou este código, ignore este e-mail.

                Equipe AgênciasHub""".formatted(userName, code);
        return new TransactionalMail("AgênciasHub — Código de Verificação", body);
    }

    public static TransactionalMail invitation(
            String inviteUrl, String agencyName, String inviterName) {
        String body = """
                Olá,

                %s convidou você para fazer parte da agência %s no AgênciasHub.

                Para aceitar o convite e criar sua conta, acesse o link abaixo:
                %s

                Este convite expira em 72 horas.

                Equipe AgênciasHub""".formatted(inviterName, agencyName, inviteUrl);
        return new TransactionalMail("AgênciasHub — Convite para %s".formatted(agencyName), body);
    }

    public static TransactionalMail passwordReset(String userName, String code) {
        String body = """
                Olá %s,

                Seu código para redefinição de senha é: %s

                Este código expira em 15 minutos.

                Se você não solicitou a redefinição, ignore este e-mail.

                Equipe AgênciasHub""".formatted(userName, code);
        return new TransactionalMail("AgênciasHub — Redefinição de Senha", body);
    }
}
