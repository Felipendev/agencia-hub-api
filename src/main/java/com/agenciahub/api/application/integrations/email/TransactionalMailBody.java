package com.agenciahub.api.application.integrations.email;

/** Single place for email copy and HTML templates. */
public final class TransactionalMailBody {

    private TransactionalMailBody() {}

    // ── Public factories ───────────────────────────────────────────────────────

    public static TransactionalMail verificationCode(
            String userName, String code, String email, String baseUrl, String linkToken) {

        String verifyUrl = baseUrl + "/cadastro/verificar?t=" + linkToken;

        String text = """
                Olá %s,

                Clique no link abaixo para ativar sua conta no AgênciasHub (válido por 15 minutos):
                %s

                Prefere digitar o código? Use: %s

                Se você não criou uma conta, ignore este e-mail.

                Equipe AgênciasHub""".formatted(userName, verifyUrl, code);

        String body = """
                <h1 style="margin:0 0 8px;font-size:24px;font-weight:700;color:#0B1B2B;
                           text-align:center;line-height:1.25;">Ative sua conta</h1>
                <p style="margin:0 0 8px;font-size:15px;color:#64748B;text-align:center;line-height:1.6;">
                  Olá, <strong style="color:#1E293B;">%s</strong>!
                </p>
                <p style="margin:0 0 32px;font-size:15px;color:#475569;text-align:center;line-height:1.7;">
                  Clique no botão abaixo para confirmar seu e-mail e começar a usar o AgênciasHub.
                  O link expira em <strong>15&nbsp;minutos</strong>.
                </p>
                %s
                <div style="margin:28px 0 0;border-top:1px solid #E2E8F0;padding-top:24px;">
                  <p style="margin:0 0 6px;font-size:13px;color:#94A3B8;text-align:center;">
                    Não consegue clicar no botão? Digite o código abaixo na tela de verificação:
                  </p>
                  <div style="margin:12px auto 0;max-width:220px;background:#F1F5F9;border-radius:10px;
                              padding:14px 20px;text-align:center;letter-spacing:12px;
                              font-size:28px;font-weight:700;color:#0B1B2B;
                              font-family:'Courier New',Courier,monospace;">%s</div>
                </div>
                """.formatted(userName, ctaButton(verifyUrl, "Ativar minha conta"), code);

        return new TransactionalMail(
                "AgênciasHub — Ative sua conta",
                text,
                layout(body, "Se você não solicitou esta ativação, ignore este e-mail com segurança.", email));
    }

    public static TransactionalMail invitation(String inviteUrl, String agencyName, String inviterName) {

        String text = """
                Olá,

                %s convidou você para fazer parte da agência %s no AgênciasHub.

                Para aceitar o convite e criar sua conta, acesse o link abaixo:
                %s

                Este convite expira em 72 horas.

                Equipe AgênciasHub""".formatted(inviterName, agencyName, inviteUrl);

        String body = """
                <h1 style="margin:0 0 8px;font-size:24px;font-weight:700;color:#0B1B2B;
                           text-align:center;line-height:1.25;">Você foi convidado!</h1>
                <p style="margin:0 0 24px;font-size:15px;color:#475569;text-align:center;line-height:1.7;">
                  <strong style="color:#1E293B;">%s</strong> convidou você para fazer parte da agência
                  <strong style="color:#1E293B;">%s</strong> no AgênciasHub.
                </p>
                <p style="margin:0 0 32px;font-size:15px;color:#475569;text-align:center;line-height:1.7;">
                  Clique no botão abaixo para aceitar o convite e criar sua conta.
                  Este convite expira em <strong>72&nbsp;horas</strong>.
                </p>
                %s
                """.formatted(inviterName, agencyName, ctaButton(inviteUrl, "Aceitar convite"));

        return new TransactionalMail(
                "AgênciasHub — Convite para %s".formatted(agencyName),
                text,
                layout(body, "Se você não esperava este convite, pode ignorar este e-mail.", null));
    }

    public static TransactionalMail passwordReset(String userName, String code) {

        String text = """
                Olá %s,

                Seu código para redefinição de senha é: %s

                Este código expira em 15 minutos.

                Se você não solicitou a redefinição, ignore este e-mail.

                Equipe AgênciasHub""".formatted(userName, code);

        String body = """
                <h1 style="margin:0 0 8px;font-size:24px;font-weight:700;color:#0B1B2B;
                           text-align:center;line-height:1.25;">Redefinição de senha</h1>
                <p style="margin:0 0 8px;font-size:15px;color:#64748B;text-align:center;line-height:1.6;">
                  Olá, <strong style="color:#1E293B;">%s</strong>!
                </p>
                <p style="margin:0 0 28px;font-size:15px;color:#475569;text-align:center;line-height:1.7;">
                  Recebemos uma solicitação para redefinir a senha da sua conta.
                  Use o código abaixo na tela de redefinição. Ele expira em <strong>15&nbsp;minutos</strong>.
                </p>
                <div style="margin:0 auto 28px;max-width:220px;background:#F1F5F9;border-radius:10px;
                            padding:16px 20px;text-align:center;letter-spacing:12px;
                            font-size:30px;font-weight:700;color:#0B1B2B;
                            font-family:'Courier New',Courier,monospace;">%s</div>
                <p style="margin:0;font-size:13px;color:#94A3B8;text-align:center;">
                  O código é válido para uso único.
                </p>
                """.formatted(userName, code);

        return new TransactionalMail(
                "AgênciasHub — Redefinição de Senha",
                text,
                layout(body, "Se você não solicitou a redefinição, ignore este e-mail. Sua senha não foi alterada.", null));
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /** Renders the full HTML wrapper around arbitrary body content. */
    private static String layout(String bodyContent, String footerDisclaimer, String recipientEmail) {
        String recipientLine = recipientEmail != null && !recipientEmail.isBlank()
                ? "<p style=\"margin:8px 0 0;font-size:12px;color:#94A3B8;\">" +
                  "Este e-mail foi enviado para " +
                  "<a href=\"mailto:" + recipientEmail + "\" " +
                  "style=\"color:#0EA5E9;text-decoration:none;\">" + recipientEmail + "</a>.</p>"
                : "";

        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width,initial-scale=1">
                  <title>AgênciasHub</title>
                </head>
                <body style="margin:0;padding:0;background:#F0F4F8;
                             font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,Helvetica,sans-serif;">

                  <table width="100%%" cellpadding="0" cellspacing="0" role="presentation">
                    <tr><td align="center" style="padding:40px 16px 48px;">

                      <!-- ── Logo (above card) ──────────────────────────────── -->
                      <table cellpadding="0" cellspacing="0" role="presentation"
                             style="margin-bottom:28px;">
                        <tr>
                          <td style="text-align:center;">
                            <img src="https://agenciashub.com.br/logo_mais_nome.png"
                                 width="180" height="44" alt="AgênciasHub"
                                 style="display:block;border:0;outline:none;text-decoration:none;">
                          </td>
                        </tr>
                      </table>

                      <!-- ── Card ──────────────────────────────────────────── -->
                      <table width="560" cellpadding="0" cellspacing="0" role="presentation"
                             style="max-width:560px;width:100%%;background:#FFFFFF;
                                    border-radius:16px;overflow:hidden;
                                    box-shadow:0 4px 24px rgba(0,0,0,0.07),0 1px 4px rgba(0,0,0,0.04);">

                        <!-- Body -->
                        <tr>
                          <td style="padding:40px 40px 36px;">
                            %s
                          </td>
                        </tr>

                        <!-- Footer -->
                        <tr>
                          <td style="background:#F8FAFC;border-top:1px solid #E2E8F0;
                                     padding:20px 40px;text-align:center;">
                            <p style="margin:0;font-size:12px;color:#94A3B8;line-height:1.6;">
                              %s
                            </p>
                            %s
                          </td>
                        </tr>

                      </table>

                      <!-- ── Bottom logo ────────────────────────────────────── -->
                      <table cellpadding="0" cellspacing="0" role="presentation"
                             style="margin-top:28px;">
                        <tr>
                          <td style="text-align:center;">
                            <img src="https://agenciashub.com.br/logo_mais_nome.png"
                                 width="120" height="30" alt="AgênciasHub"
                                 style="display:inline-block;border:0;outline:none;text-decoration:none;">
                          </td>
                        </tr>
                        <tr>
                          <td style="padding-top:8px;text-align:center;">
                            <span style="font-size:11px;color:#CBD5E1;">
                              &copy; 2025 AgênciasHub. Todos os direitos reservados.
                            </span>
                          </td>
                        </tr>
                      </table>

                    </td></tr>
                  </table>

                </body>
                </html>""".formatted(bodyContent, footerDisclaimer, recipientLine);
    }

    public static TransactionalMail newSubmissionAlert(
            String agencyName, String clienteNome, String telefone,
            String rota, String datas, String dashboardUrl, String recipientEmail) {

        String text = """
                Nova solicitação de orçamento recebida em %s.

                Cliente: %s
                Telefone: %s
                Rota: %s
                Datas: %s

                Acesse o painel para visualizar e importar: %s

                Equipe AgênciasHub""".formatted(agencyName, clienteNome, telefone, rota, datas, dashboardUrl);

        String row = "<tr><td style=\"padding:6px 0;font-size:14px;color:#475569;\"><strong style=\"color:#1E293B;\">%s</strong> %s</td></tr>";

        String body = """
                <h1 style="margin:0 0 16px;font-size:22px;font-weight:700;color:#0B1B2B;
                           text-align:center;line-height:1.25;">Nova solicitação recebida</h1>
                <p style="margin:0 0 24px;font-size:15px;color:#475569;text-align:center;line-height:1.6;">
                  Um cliente preencheu o formulário público de <strong style="color:#1E293B;">%s</strong>.
                </p>
                <table cellpadding="0" cellspacing="0" role="presentation"
                       style="width:100%%;border-top:1px solid #E2E8F0;margin-bottom:28px;">
                  %s
                  %s
                  %s
                  %s
                </table>
                %s
                """.formatted(
                agencyName,
                row.formatted("Cliente:", clienteNome),
                row.formatted("Telefone:", telefone),
                row.formatted("Rota:", rota),
                row.formatted("Datas:", datas),
                ctaButton(dashboardUrl, "Ver no painel"));

        return new TransactionalMail(
                "Nova solicitação de orçamento — " + agencyName,
                text,
                layout(body,
                        "Você recebe este e-mail porque é gestor(a) da agência " + agencyName + " no AgênciasHub.",
                        recipientEmail));
    }

    /** Centered CTA button following the brand amber color. */
    private static String ctaButton(String href, String label) {
        return """
                <table cellpadding="0" cellspacing="0" role="presentation"
                       style="margin:0 auto;">
                  <tr>
                    <td style="background:#E0A458;border-radius:9px;">
                      <a href="%s"
                         style="display:inline-block;padding:14px 40px;
                                color:#0B1B2B;font-size:15px;font-weight:700;
                                text-decoration:none;letter-spacing:0.2px;
                                border-radius:9px;">
                        %s
                      </a>
                    </td>
                  </tr>
                </table>""".formatted(href, label);
    }

    public static TransactionalMail dataDeletionConfirmation(String toEmail) {
        String text = """
                Olá,

                Recebemos sua solicitação de exclusão de dados pessoais no AgênciasHub.

                Analisaremos sua solicitação em até 15 dias úteis e enviaremos uma resposta para este endereço de e-mail.

                Caso não tenha feito esta solicitação, entre em contato conosco.

                Equipe AgênciasHub""";

        String body = """
                <h1 style="margin:0 0 8px;font-size:22px;font-weight:700;color:#0B1B2B;
                           text-align:center;">Solicitação de exclusão recebida</h1>
                <p style="margin:16px 0;font-size:15px;color:#475569;text-align:center;line-height:1.7;">
                  Recebemos sua solicitação de exclusão de dados pessoais.<br>
                  Analisaremos em até <strong>15 dias úteis</strong> e retornaremos neste e-mail.
                </p>""";

        return new TransactionalMail(
                "AgênciasHub — Solicitação de exclusão de dados recebida",
                text,
                layout(body, "Caso não tenha feito esta solicitação, ignore este e-mail.", toEmail));
    }

    public static TransactionalMail dataDeletionOwnerNotification(String processUrl) {
        String text = """
                Olá,

                Uma nova solicitação de exclusão de dados foi recebida. Acesse o painel para analisá-la:
                %s

                Equipe AgênciasHub""".formatted(processUrl);

        String body = """
                <h1 style="margin:0 0 8px;font-size:22px;font-weight:700;color:#0B1B2B;
                           text-align:center;">Nova solicitação de exclusão</h1>
                <p style="margin:16px 0 32px;font-size:15px;color:#475569;text-align:center;line-height:1.7;">
                  Uma nova solicitação de exclusão de dados foi recebida.<br>
                  Acesse o painel para analisar e processar.
                </p>
                %s""".formatted(ctaButton(processUrl, "Ver solicitação"));

        return new TransactionalMail(
                "AgênciasHub — Nova solicitação de exclusão de dados",
                text,
                layout(body, null, null));
    }

    public static TransactionalMail dataDeletionProcessed(boolean accepted, String justificativa) {
        String titulo = accepted ? "Seus dados foram excluídos" : "Solicitação de exclusão processada";
        String mensagem = accepted
                ? "Seus dados pessoais foram excluídos da nossa plataforma conforme solicitado."
                : "Sua solicitação de exclusão foi analisada. " +
                  (justificativa != null && !justificativa.isBlank()
                          ? "Motivo: " + justificativa
                          : "Não foi possível processar a exclusão neste momento.");

        String text = """
                Olá,

                %s

                Equipe AgênciasHub""".formatted(mensagem);

        String body = """
                <h1 style="margin:0 0 8px;font-size:22px;font-weight:700;color:#0B1B2B;
                           text-align:center;">%s</h1>
                <p style="margin:16px 0;font-size:15px;color:#475569;text-align:center;line-height:1.7;">%s</p>
                """.formatted(titulo, mensagem);

        return new TransactionalMail(
                "AgênciasHub — " + titulo,
                text,
                layout(body, null, null));
    }
}
