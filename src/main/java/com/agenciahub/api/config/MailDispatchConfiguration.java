package com.agenciahub.api.config;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.agenciahub.api.service.email.TransactionalMail;
import com.agenciahub.api.service.email.TransactionalMailChannel;

/**
 * Picks one {@link TransactionalMailChannel}. To add a provider: implement the interface and add a branch here.
 */
@Configuration
public class MailDispatchConfiguration {

    @Bean
    TransactionalMailChannel transactionalMailChannel(
            @Value("${email.resend.api-key:}") String resendApiKey,
            @Value("${email.from:contato@agenciashub.com.br}") String fromAddress,
            @Value("${email.resend.connect-timeout-ms:15000}") int resendConnectMs,
            @Value("${email.resend.read-timeout-ms:30000}") int resendReadMs,
            @Value("${spring.mail.host:}") String smtpHost,
            @Value("${spring.mail.password:}") String smtpPassword,
            @Autowired(required = false) JavaMailSender javaMailSender) {

        if (StringUtils.hasText(resendApiKey.trim())) {
            return new ResendChannel(
                    resendApiKey.trim(), fromAddress, resendConnectMs, resendReadMs);
        }
        if (StringUtils.hasText(smtpHost)
                && StringUtils.hasText(smtpPassword)
                && javaMailSender != null) {
            return new SmtpChannel(javaMailSender, fromAddress);
        }
        return new LoggingChannel();
    }

    private static final class ResendChannel implements TransactionalMailChannel {

        private static final Logger log = LoggerFactory.getLogger(ResendChannel.class);
        private static final String BASE = "https://api.resend.com";

        private final RestClient client;
        private final String from;

        ResendChannel(String apiKey, String from, int connectMs, int readMs) {
            this.from = from;
            SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
            rf.setConnectTimeout(connectMs);
            rf.setReadTimeout(readMs);
            this.client = RestClient.builder()
                    .baseUrl(BASE)
                    .requestFactory(rf)
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .build();
        }

        @Override
        public void send(String to, TransactionalMail mail) {
            try {
                client.post()
                        .uri("/emails")
                        .body(Map.of(
                                "from", from,
                                "to", List.of(to),
                                "subject", mail.subject(),
                                "text", mail.textBody()))
                        .retrieve()
                        .toBodilessEntity();
                log.debug("Resend: sent to {} subject '{}'", to, mail.subject());
            } catch (RestClientException e) {
                log.warn("Resend: failed to {}: {}", to, e.getMessage(), e);
            }
        }
    }

    private static final class SmtpChannel implements TransactionalMailChannel {

        private static final Logger log = LoggerFactory.getLogger(SmtpChannel.class);

        private final JavaMailSender mailSender;
        private final String from;

        SmtpChannel(JavaMailSender mailSender, String from) {
            this.mailSender = mailSender;
            this.from = from;
        }

        @Override
        public void send(String to, TransactionalMail mail) {
            try {
                SimpleMailMessage m = new SimpleMailMessage();
                m.setFrom(from);
                m.setTo(to);
                m.setSubject(mail.subject());
                m.setText(mail.textBody());
                mailSender.send(m);
                log.debug("SMTP: sent to {} subject '{}'", to, mail.subject());
            } catch (Exception e) {
                log.warn("SMTP: failed to {}: {}", to, e.getMessage(), e);
            }
        }
    }

    private static final class LoggingChannel implements TransactionalMailChannel {

        private static final Logger log = LoggerFactory.getLogger(LoggingChannel.class);
        private static final Pattern SIX_DIGIT_CODE = Pattern.compile("\\b\\d{6}\\b");

        @Override
        public void send(String to, TransactionalMail mail) {
            Matcher matcher = SIX_DIGIT_CODE.matcher(mail.textBody());
            if (matcher.find()) {
                log.info("[EMAIL-NOOP] to={} subject={} code={}", to, mail.subject(), matcher.group());
                return;
            }
            log.info("[EMAIL-NOOP] to={} subject={} body={}", to, mail.subject(), mail.textBody());
        }
    }
}
