# ADR 0006 — Códigos de verificação, link público e job de trial

## Status

Aceite (implementado no repositório).

## Contexto

Com a remoção das fachadas `*Service` por agregado HTTP, restavam três tipos em `com.agenciahub.api.service`:

- Códigos de verificação (e-mail / reset) + e-mail
- Geração de `public_link_code` para utilizadores
- Job agendado de expiração de trial

## Decisão

1. **Códigos de verificação:** porta **`VerificationCodePort`** + **`DefaultVerificationCodeService`** em **`application.integrations.verification`** (usa `EmailService` de `integrations.email`).
2. **Link público:** **`PublicLinkCodeSupport`** em **`application.usecases.user`** (componente partilhado pelos use cases de auth/user que precisam alocar ou garantir código).
3. **Trial expirado:** **`ExpireTrialsScheduler`** em **`application.scheduling`** (substitui `TrialSchedulerService`).

## Consequências

- **`com.agenciahub.api.service`** deixa de existir no código de produção.
- Use cases de auth injetam `VerificationCodePort` e `PublicLinkCodeSupport` em vez de fachadas em `service`.

## Alternativas não escolhidas

- Absorver toda a lógica de códigos dentro de cada use case de auth: duplicação de rate limit e BCrypt.
