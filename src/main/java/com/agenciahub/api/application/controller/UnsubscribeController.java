package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.security.UnsubscribeTokenService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * NOTIF-02: stateless email unsubscribe endpoint.
 * GET /public/unsubscribe?t={token}&type={notifType}
 */
@RestController
@RequestMapping("/public/unsubscribe")
@RequiredArgsConstructor
public class UnsubscribeController {

    private final UnsubscribeTokenService tokenService;
    private final PlatformAccountRepository userRepository;

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> unsubscribe(
            @RequestParam("t") String token,
            @RequestParam("type") String type) {

        UnsubscribeTokenService.UnsubscribeClaims claims;
        try {
            claims = tokenService.parse(token);
        } catch (JwtException | IllegalArgumentException e) {
            return ok(errorPage());
        }

        if (!type.equals(claims.notifType())) {
            return ok(errorPage());
        }

        Optional<PlatformAccount> userOpt = userRepository.findByEmail(claims.email());
        if (userOpt.isPresent()) {
            PlatformAccount user = userOpt.get();
            applyUnsubscribe(user, type);
            userRepository.save(user);
            return ok(successPage(type));
        }
        return ok(successPage(type));
    }

    private void applyUnsubscribe(PlatformAccount user, String type) {
        switch (type) {
            case "submissao"           -> user.setNotifEmailSubmissao(false);
            case "cotacao_aprovada"    -> user.setNotifEmailCotacaoAprovada(false);
            case "cotacao_vencendo"    -> user.setNotifEmailCotacaoVencendo(false);
            case "cotacao_vencida"     -> user.setNotifEmailCotacaoVencida(false);
            case "exclusao_agendada"   -> user.setNotifEmailExclusaoAgendada(false);
            default -> { /* unknown type — ignore */ }
        }
    }

    private static ResponseEntity<String> ok(String html) {
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    private static String successPage(String type) {
        String label = switch (type) {
            case "submissao"         -> "novas solicitações";
            case "cotacao_aprovada"  -> "cotação aprovada";
            case "cotacao_vencendo"  -> "cotação vencendo";
            case "cotacao_vencida"   -> "cotação vencida";
            case "exclusao_agendada" -> "exclusão agendada";
            default -> "este tipo de notificação";
        };
        return page("Descadastro realizado",
                "Você não receberá mais e-mails sobre " + label + ".",
                "Suas preferências de notificação foram atualizadas com sucesso.");
    }

    private static String errorPage() {
        return page("Link inválido",
                "Este link de descadastro é inválido ou expirou.",
                "Para gerenciar suas preferências, acesse o painel.");
    }

    private static String page(String title, String heading, String body) {
        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head><meta charset="UTF-8"><title>AgênciasHub — %s</title>
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <style>body{font-family:sans-serif;display:flex;justify-content:center;
                align-items:center;min-height:100vh;margin:0;background:#F0F4F8}
                .card{max-width:480px;padding:40px;background:#fff;border-radius:16px;
                box-shadow:0 4px 24px rgba(0,0,0,.07);text-align:center}
                h1{color:#0B1B2B;font-size:22px}p{color:#475569;line-height:1.6}</style>
                </head>
                <body><div class="card">
                <h1>%s</h1><p>%s</p>
                </div></body></html>
                """.formatted(title, heading, body);
    }
}
