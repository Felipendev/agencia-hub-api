# ADR 0003 — Contrato de erros HTTP, OpenAPI e contexto (tenant / segurança)

## Status

Aceito — refletido no código (`agencia-hub-api`).

## Contexto

- Clientes e integrações precisam de **corpo de erro JSON estável** (código de negócio + mensagem), alinhado à documentação OpenAPI.
- A API é **multi-tenant por agência**: parte dos endpoints exige agência resolvida no request; faltas de autenticação ou de contexto não devem virar **404** genérico.
- Documentação Swagger por recurso repetia manualmente as mesmas respostas 400/401/403/404/409/500.

## Decisão

1. **Payload de erro:** tipo `ApiError` (campos acordados no projeto) exposto no OpenAPI como schema reutilizável (`OpenApiConfig`); descrição do `Info` lista o mapeamento **HTTP ↔ `code`** (`UNAUTHENTICATED`, `MISSING_AGENCY_CONTEXT`, `NOT_FOUND`, `VALIDATION_ERROR`, etc.) para leitura humana.
2. **Handler global:** `GlobalExceptionHandler` em `com.agenciahub.api.web` (fora de `controller`), centralizando tradução de exceções para `ApiError` e status HTTP.
3. **OpenAPI por recurso:** meta-anotação `@StandardErrorApiResponses` em `com.agenciahub.api.application.controllers.docs`, aplicada em **tipo** nas interfaces `*API` que declaram o contrato REST — documenta respostas de erro com schema `ApiError` sem duplicar texto em cada método. Operações podem manter `@ApiResponses` adicionais (ex.: 200/204 ou 404 semântico) no método.
4. **Segurança:** `SecurityContextUsers` concentra leitura do `User` a partir do JWT (`optionalUser`, `requireUserId`, `requireUser`). Ausência de contexto utilizável para operações que exigem usuário → **`UnauthenticatedException`** → **401** com código `UNAUTHENTICATED` (não usar 404 para “não autenticado”).
5. **Tenant:** onde o fluxo depende da agência no `ThreadLocal`, usar `TenantContext.requireAgencyId()`; ausência explícita → **`MissingAgencyContextException`** → **403** com código `MISSING_AGENCY_CONTEXT`.

## Consequências

### Positivas

- Contrato de erro e documentação Swagger **consistentes** entre recursos.
- Comportamento de auth/tenant **previsível** para o cliente e para testes (`GlobalExceptionHandlerTest`, etc.).
- Novas interfaces `*API` devem incluir `@StandardErrorApiResponses` (ou equivalente documentado) para não regredir o padrão.

### Negativas / cuidados

- Endpoints **públicos** também herdam a documentação de 401/403; em alguns paths isso é raro na prática — aceitável como “formato de erro possível do stack”, ou refinar por método se incomodar.
- Alteração de códigos ou de status exige **atualizar** `OpenApiConfig`, `ApiError` (se aplicável), handler e este ADR em conjunto.

## Alternativas não escolhidas

- Documentar apenas erros no `Info` global, sem por operação — rejeitado por pouca visibilidade no Swagger por endpoint.
- Manter resolução de usuário/tenant duplicada em controllers — rejeitado por divergência e bugs (ex.: 404 vs 401).
