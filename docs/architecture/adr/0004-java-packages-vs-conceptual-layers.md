# ADR 0004 — Pacotes Java (`controller`, `application`, `service`) vs camadas conceptuais

## Status

**Aceito (histórico)** — mapeamento conceptual pacote → camada.

> **Supersedido em parte:**
> - **HTTP:** **ADR 0007** — `application.controller` + `application.controller.doc` (não mais `controller` na raiz nem `application.controllers.docs`).
> - **Persistência JPA:** **ADR 0009** — `application.persistence.entity` / `.repository` (não mais `entity` / `repository` na raiz).
> - **Serviços monolíticos:** eliminados (roadmap Fase 1); use cases em `application.usecases.*`.

## Contexto

- O `02-backend-architecture.md` descreve **quatro áreas conceptuais**: API, Application, Domain, Infrastructure.
- Em muitos exemplos didáticos ou templates, tudo o que é “aplicação” aparece sob um único prefixo (ex.: `application/controllers`, `application/usecases`, `application/persistence`), o que **espelha literalmente** a árvore de pastas às caixas do diagrama.
- Neste projeto, os `@RestController` vivem em **`com.agenciahub.api.controller.<feature>`**, as interfaces `*API` (OpenAPI) em **`com.agenciahub.api.application.controller.doc`**, os casos de uso em **`com.agenciahub.api.application.<feature>`**, e a orquestração transacional / acesso a dados continua em grande parte em **`com.agenciahub.api.service`** (fachadas que os use cases chamam), com entidades e repositórios JPA em `entity` / `repository`.

Sem documentar essa escolha, parece **incoerência** entre o diagrama (API ≠ Application) e os pacotes (dois irmãos `controller` e `application` em vez de `application/controllers`).

## Decisão

1. **Manter a coerência por responsabilidade, não por espelho literal da árvore do diagrama.** A regra é: *cada pacote deve ter um papel claro*; os nomes dos pacotes **não** precisam repetir os rótulos em inglês das caixas do desenho.
2. **Mapeamento acordado (pacote → camada):**
   - `com.agenciahub.api.controller.<feature>` → **camada API** (HTTP, adaptação request/response; `@RestController`).
   - `com.agenciahub.api.application.controller.doc` → **contratos OpenAPI** (`*API`, meta-anotações partilhadas); os controllers **implementam** estas interfaces.
   - `com.agenciahub.api.application.<feature>` → **camada Application** (casos de uso, comandos de aplicação, mappers de resposta dedicados ao fluxo).
   - `com.agenciahub.api.service` → **orquestração + persistência via Spring** na transição incremental (fachadas chamadas pelos use cases; **não** é “domínio puro”).
   - `com.agenciahub.api.application.persistence.entity` / `.repository` → persistência JPA (**ADR 0009**); domínio rico separado continua fora de escopo.
   - `com.agenciahub.api.web`, `config`, `security` → infraestrutura transversal ou composição Spring, conforme já documentado noutros ficheiros.
3. **Não mover classes `@RestController` para dentro de `application`** só para coincidir com um template externo. O subpacote `application.controllers.docs` existe **apenas** para contratos OpenAPI (interfaces), não para beans MVC. Qualquer reorganização maior (ex.: `application/controllers` com classes de runtime, renomear `service` em massa) exige **ADR novo**, plano de PRs e custo explícito — está **fora** do roadmap incremental salvo decisão do time.

## Consequências

### Positivas

- O diagrama conceptual continua válido; a **dúvida “onde ponho isto?”** fica respondida sem obrigar a um mega-rename.
- Novos módulos seguem o padrão já consolidado: `controller.<feature>` + `application.controllers.docs` (`*API`) + `application.<feature>` + delegação a serviços existentes ou novos.

### Negativas / limitações

- Quem espera **uma pasta `application` com subpastas `controllers` (classes)** / `usecases` não as encontra para MVC; contratos OpenAPI estão em `application.controllers.docs`. É preciso ler este ADR, o **`06-clean-architecture-use-case-driven.md`** (mapeamento alvo) e o **`04-incremental-refactoring-roadmap.md`** (fases pós-baseline).
- A pasta `service` continua a ser um “catch-all” operacional até haver piloto para extrair ports para `infrastructure` (sem promessa de prazo neste ADR).

## Alternativas não escolhidas (por agora)

- Reorganizar tudo sob `application/{controllers,usecases,persistence}` para coincidir com um diagrama genérico — **rejeitado** no estado atual por custo, risco de regressão e baixo ganho imediato face ao roadmap incremental.
- Renomear `com.agenciahub.api.controller` para `com.agenciahub.api.api` ou `http` — possível no futuro com ADR; **não** é decisão deste registo.
