# ADR 0004 — Pacotes Java (`controller`, `application`, `service`) vs camadas conceptuais

## Status

Aceito — descreve o estado desejado do repositório `agencia-hub-api` (incremental).

## Contexto

- O `02-backend-architecture.md` descreve **quatro áreas conceptuais**: API, Application, Domain, Infrastructure.
- Em muitos exemplos didáticos ou templates, tudo o que é “aplicação” aparece sob um único prefixo (ex.: `application/controllers`, `application/usecases`, `application/persistence`), o que **espelha literalmente** a árvore de pastas às caixas do diagrama.
- Neste projeto, a API HTTP (Spring `@RestController` + interfaces `*API`) vive em **`com.agenciahub.api.controller.<feature>`**, os casos de uso em **`com.agenciahub.api.application.<feature>`**, e a orquestração transacional / acesso a dados continua em grande parte em **`com.agenciahub.api.service`** (fachadas que os use cases chamam), com entidades e repositórios JPA em `entity` / `repository`.

Sem documentar essa escolha, parece **incoerência** entre o diagrama (API ≠ Application) e os pacotes (dois irmãos `controller` e `application` em vez de `application/controllers`).

## Decisão

1. **Manter a coerência por responsabilidade, não por espelho literal da árvore do diagrama.** A regra é: *cada pacote deve ter um papel claro*; os nomes dos pacotes **não** precisam repetir os rótulos em inglês das caixas do desenho.
2. **Mapeamento acordado (pacote → camada):**
   - `com.agenciahub.api.controller.<feature>` (+ `…docs` para `*API`) → **camada API** (HTTP, OpenAPI, adaptação request/response).
   - `com.agenciahub.api.application.<feature>` → **camada Application** (casos de uso, comandos de aplicação, mappers de resposta dedicados ao fluxo).
   - `com.agenciahub.api.service` → **orquestração + persistência via Spring** na transição incremental (fachadas chamadas pelos use cases; **não** é “domínio puro”).
   - `com.agenciahub.api.entity` / `repository` → persistência acoplada ao JPA (evolução futura para ports/adapters exige **outro ADR** e piloto).
   - `com.agenciahub.api.web`, `config`, `security` → infraestrutura transversal ou composição Spring, conforme já documentado noutros ficheiros.
3. **Não mover controllers para dentro de `application`** só para coincidir com um template externo. Qualquer reorganização grande (ex.: `application/controllers`, renomear `service` em massa) exige **ADR novo**, plano de PRs e custo explícito — está **fora** do roadmap incremental salvo decisão do time.

## Consequências

### Positivas

- O diagrama conceptual continua válido; a **dúvida “onde ponho isto?”** fica respondida sem obrigar a um mega-rename.
- Novos módulos seguem o padrão já consolidado: `controller.<feature>` + `application.<feature>` + delegação a serviços existentes ou novos.

### Negativas / limitações

- Quem espera **uma pasta `application` com subpastas `controllers` / `usecases`** não as encontra; precisa ler este ADR ou o `02-backend-architecture.md` (secção *OpenAPI contracts* / layout alvo).
- A pasta `service` continua a ser um “catch-all” operacional até haver piloto para extrair ports para `infrastructure` (sem promessa de prazo neste ADR).

## Alternativas não escolhidas (por agora)

- Reorganizar tudo sob `application/{controllers,usecases,persistence}` para coincidir com um diagrama genérico — **rejeitado** no estado atual por custo, risco de regressão e baixo ganho imediato face ao roadmap incremental.
- Renomear `com.agenciahub.api.controller` para `com.agenciahub.api.api` ou `http` — possível no futuro com ADR; **não** é decisão deste registo.
