# ADR 0005 — E-mail outbound em `application.integrations.email`

## Status

Aceite (implementado no repositório).

## Contexto

O roadmap **Fase 3** pede integrações outbound (e-mail, HTTP, filas) fora de `com.agenciahub.api.service` genérico, alinhado ao **06** (`application/integrations/{service}/`).

## Decisão

- A porta **`EmailService`** e a implementação **`DefaultEmailService`** passam a viver em **`com.agenciahub.api.application.integrations.email`**.
- Tipos de transporte (`TransactionalMail`, `TransactionalMailBody`, `TransactionalMailChannel`) acompanham o mesmo pacote.
- A escolha de canal (Resend / SMTP / log) mantém-se em **`MailDispatchConfiguration`** (infra Spring), que instancia `TransactionalMailChannel` injetado no `DefaultEmailService`.

## Consequências

- Use cases que enviam e-mail (ex.: convites) importam `EmailService` a partir de `integrations.email`.
- **`VerificationCodeService`** continua em `service` mas depende da porta `EmailService` em `integrations` (aceitável até eventual extração do próprio serviço de códigos).

## Alternativas não escolhidas

- Renomear a porta para `EmailIntegration` / `MailPort`: possível evolução; mantivemos o nome **`EmailService`** na nova localização para reduzir ruído nas assinaturas.
