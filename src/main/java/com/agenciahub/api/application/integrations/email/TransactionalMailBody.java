package com.agenciahub.api.application.integrations.email;

/** Single place for Portuguese copy and HTML templates. */
public final class TransactionalMailBody {

    private TransactionalMailBody() {}

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

        String html = """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width,initial-scale=1">
                  <title>Ative sua conta — AgênciasHub</title>
                </head>
                <body style="margin:0;padding:0;background:#eef2f7;font-family:Arial,Helvetica,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" role="presentation"
                         style="background:#eef2f7;padding:40px 16px;">
                    <tr><td align="center">
                      <table width="560" cellpadding="0" cellspacing="0" role="presentation"
                             style="max-width:560px;background:#ffffff;border-radius:12px;
                                    overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,0.08);">

                        <!-- ── Header ──────────────────────────────────────── -->
                        <tr>
                          <td style="background:#0f2840;padding:24px 32px;">
                            <table cellpadding="0" cellspacing="0" role="presentation">
                              <tr>
                                <td style="background:#f5c518;border-radius:7px;width:36px;height:36px;
                                           text-align:center;vertical-align:middle;">
                                  <span style="font-weight:900;font-size:11px;color:#0f2840;
                                               line-height:36px;display:block;">AH</span>
                                </td>
                                <td style="padding-left:10px;">
                                  <span style="color:#ffffff;font-size:17px;font-weight:700;
                                               letter-spacing:-0.3px;">AgênciasHub</span>
                                </td>
                              </tr>
                            </table>
                          </td>
                        </tr>

                        <!-- ── Body ───────────────────────────────────────── -->
                        <tr>
                          <td style="padding:40px 32px 32px;">
                            <h1 style="margin:0 0 6px;font-size:22px;font-weight:700;
                                       color:#0f2840;line-height:1.25;">Ative sua conta</h1>
                            <p style="margin:0 0 20px;font-size:15px;color:#5a6478;line-height:1.6;">
                              Olá, <strong style="color:#374151;">%s</strong>!
                            </p>
                            <p style="margin:0 0 28px;font-size:15px;color:#374151;line-height:1.65;">
                              Clique no botão abaixo para confirmar seu e-mail e acessar o AgênciasHub.
                              O link expira em <strong>15&nbsp;minutos</strong>.
                            </p>

                            <!-- Button -->
                            <table cellpadding="0" cellspacing="0" role="presentation"
                                   style="margin:0 0 32px;">
                              <tr>
                                <td style="background:#f5c518;border-radius:8px;">
                                  <a href="%s"
                                     style="display:inline-block;padding:14px 36px;
                                            color:#0f2840;font-size:15px;font-weight:700;
                                            text-decoration:none;letter-spacing:0.2px;">
                                    Ativar minha conta &rarr;
                                  </a>
                                </td>
                              </tr>
                            </table>

                            <!-- Code fallback -->
                            <p style="margin:0 0 10px;font-size:13px;color:#8a95a3;">
                              Não consegue clicar no botão? Use o código abaixo na tela de verificação:
                            </p>
                            <div style="background:#eef2f7;border-radius:8px;padding:16px;
                                        text-align:center;letter-spacing:10px;
                                        font-size:28px;font-weight:700;color:#0f2840;
                                        font-family:'Courier New',monospace;">
                              %s
                            </div>
                          </td>
                        </tr>

                        <!-- ── Footer ──────────────────────────────────────── -->
                        <tr>
                          <td style="background:#f8fafc;border-top:1px solid #e5e9ee;
                                     padding:20px 32px;text-align:center;">
                            <p style="margin:0 0 4px;font-size:12px;color:#9ca3af;">
                              Se você não solicitou esta ativação, ignore este e-mail com segurança.
                            </p>
                            <p style="margin:0;font-size:12px;color:#9ca3af;">
                              &copy; 2025 AgênciasHub. Todos os direitos reservados.
                            </p>
                          </td>
                        </tr>

                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>""".formatted(userName, verifyUrl, code);

        return new TransactionalMail("AgênciasHub — Ative sua conta", text, html);
    }

    public static TransactionalMail invitation(String inviteUrl, String agencyName, String inviterName) {
        String text = """
                Olá,

                %s convidou você para fazer parte da agência %s no AgênciasHub.

                Para aceitar o convite e criar sua conta, acesse o link abaixo:
                %s

                Este convite expira em 72 horas.

                Equipe AgênciasHub""".formatted(inviterName, agencyName, inviteUrl);
        return new TransactionalMail("AgênciasHub — Convite para %s".formatted(agencyName), text);
    }

    public static TransactionalMail passwordReset(String userName, String code) {
        String text = """
                Olá %s,

                Seu código para redefinição de senha é: %s

                Este código expira em 15 minutos.

                Se você não solicitou a redefinição, ignore este e-mail.

                Equipe AgênciasHub""".formatted(userName, code);
        return new TransactionalMail("AgênciasHub — Redefinição de Senha", text);
    }
}
