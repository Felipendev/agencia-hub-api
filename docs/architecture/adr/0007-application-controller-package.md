# ADR 0007 — Controllers e contratos OpenAPI em `application.controller`

## Status

**Aceito** — migração mecânica aplicada (`application.controller` plano + `application.controller.doc`).

## Contexto

Após a refatoração incremental (use cases em `application.usecases`, integrações em `application.integrations`, sem `com.agenciahub.api.service`), a camada HTTP ficou **partida em dois sítios**:

| Sítio actual | Conteúdo |
|--------------|----------|
| `com.agenciahub.api.controller.<feature>` | Classes `@RestController` (runtime MVC) |
| `com.agenciahub.api.application.controllers.docs` | Interfaces `*API`, `@StandardErrorApiResponses`, OpenAPI |

Isto contradiz o modelo mental do **06** (apresentação HTTP como parte da aplicação) e confunde quem procura “onde está o controller de cotações?” — há que saltar entre a raiz `controller` e `application.controllers.docs` (plural inconsistente com `usecases` no singular conceptual).

O **ADR 0004** aceitou de propósito **não** mover `@RestController` para `application` para evitar um big-bang durante o roadmap de orquestração. Com a Fase 1–3 concluída, o custo de alinhar a **árvore de pacotes da API** é aceitável e traz benefício claro de navegação.

## Decisão

1. **Unificar a camada HTTP (apresentação) sob `com.agenciahub.api.application.controller`.**
2. **Contratos OpenAPI apenas em subpacote fixo `doc`** (singular), não misturados com classes de runtime:
   - `com.agenciahub.api.application.controller.doc` → interfaces `*API`, `StandardErrorApiResponses`, `package-info` OpenAPI.
3. **Controllers no pacote plano** `application.controller` (sem subpastas por feature): um ficheiro por classe (`CustomerController.java`, `AuthController.java`, …), cada um **implementa** a interface homónima em `application.controller.doc`.
4. **Eliminar** após migração:
   - `com.agenciahub.api.controller` (pacote raiz)
   - `com.agenciahub.api.application.controllers.docs` (nome antigo; plural `controllers` deixa de ser usado)
5. **Regra para código novo:** todo fluxo HTTP novo cria `XxxAPI` em `application.controller.doc` e `XxxController` em `application.controller` (mesmo nível); o controller importa `com.agenciahub.api.application.controller.doc.XxxAPI`.
6. **Testes:** espelhar o pacote de produção em `src/test/java/com/agenciahub/api/application/controller/...` (WebMvcTest continua a apontar para a classe do controller no novo pacote).
7. **Sem alteração de contrato REST:** apenas `package`, `import` e paths de ficheiro; paths HTTP, DTOs e comportamento permanecem iguais.

### Árvore alvo (exemplo)

```
com.agenciahub.api.application/
├── controller/
│   ├── doc/
│   │   ├── AuthAPI.java
│   │   ├── CustomerAPI.java
│   │   ├── StandardErrorApiResponses.java
│   │   └── package-info.java
│   ├── AuthController.java
│   ├── CustomerController.java
│   ├── QuotationController.java
│   └── PublicSolicitacaoConfigController.java
├── usecases/...
├── integrations/...
└── scheduling/...
```

### Mapeamento (migração)

| De | Para |
|----|------|
| `com.agenciahub.api.controller.<path>.XxxController` | `com.agenciahub.api.application.controller.XxxController` |
| `com.agenciahub.api.application.controllers.docs.XxxAPI` | `com.agenciahub.api.application.controller.doc.XxxAPI` |
| `com.agenciahub.api.application.controllers.docs.StandardErrorApiResponses` | `com.agenciahub.api.application.controller.doc.StandardErrorApiResponses` |

## Relação com outros ADRs

- **Supersedes (parcialmente) o ADR 0004** nos pontos que fixam `controller` na raiz e `application.controllers.docs` — apenas para organização de pacotes HTTP; o mapeamento conceptual API vs Application no diagrama mantém-se (controllers = adaptadores HTTP).
- **Complementa** ADR 0003 (`@StandardErrorApiResponses` continua no pacote `doc`).
- **Não altera** ADR 0005/0006 (integrações e scheduling).

## Plano de migração (um PR mecânico recomendado)

1. Mover ficheiros `application/controllers/docs/*` → `application/controller/doc/`.
2. Mover ficheiros `controller/**` → `application/controller/` (achatar subpastas: `controller/auth/AuthController` → `application/controller/AuthController`).
3. Atualizar `package` e `import` em main e test.
4. Atualizar documentação: `02`, `05`, `06`, `04` (baseline), `.cursor/rules.md`, `package-info` em `doc`.
5. Marcar ADR 0004 com nota “controllers: ver ADR 0007”.
6. `mvn test` verde; rever OpenAPI/Swagger (scan de interfaces `*API` inalterado em comportamento).

**Pronto quando:** não existir referência compilável a `com.agenciahub.api.controller` nem a `application.controllers.docs`; grep no repo só encontra histórico em ADRs antigos ou notas de supersessão.

## Consequências

### Positivas

- Uma única raiz `application` para **tudo** o que é entrada/saída HTTP + orquestração (`usecases`, `integrations`).
- `doc` deixa explícito: só contratos; não há `@RestController` misturado com interfaces OpenAPI.
- Alinhamento com `application.usecases.{feature}.{action}` (mesmo prefixo `application`).

### Negativas

- PR de rename grande (muitos ficheiros); diff ruidoso mas baixo risco se for só movimento + imports.
- Links/bookmarks IDE antigos quebrem até o time atualizar.

## Alternativas não escolhidas

- Manter `controller` na raiz e só renomear `controllers.docs` → `controller.doc` — **rejeitado**; mantém a duplicação conceptual que o time quer eliminar.
- Colocar `*API` ao lado de cada controller em `controller/<feature>/XxxAPI.java` — **rejeitado**; mistura contrato OpenAPI com bean MVC no mesmo pacote de feature e duplica o padrão `doc` centralizado.
- Pacote `application.presentation` ou `application.http` — **rejeitado** por agora; `controller` já é vocabulário estabelecido no projeto e no Spring.
