# Roadmap de refatoração incremental (backend)

Este documento define a **sequência a executar a partir de agora**, alinhada a **`02-backend-architecture.md`**, **`05-package-refactoring-and-class-responsibilities.md`** e **`06-clean-architecture-use-case-driven.md`**. O trabalho já feito no repositório conta como **Fase 0 (baseline)** — **não** deve ser repetido como checklist obrigatório.

**Princípios (sempre válidos):**

- Um passo por PR quando possível; `mvn test` verde antes de merge.
- Novo padrão só vira convenção depois de **um** piloto bem feito e revisado.
- Contrato HTTP só muda com acordo explícito (produto + API).

---

## Fase 0 — Baseline já entregue **(não reexecutar)**

Estado consolidado no `agencia-hub-api` (referência histórica; novos fluxos partem daqui):

- **Documentação:** `01`–`03`, **`05`**, **`06`**, `02` alinhado; **ADR 0003** (erros, OpenAPI, tenant/segurança), **ADR 0004** (pacotes `controller` / `application`); `.cursor/rules.md`.
- **Apresentação HTTP:** `*API` em `com.agenciahub.api.application.controllers.docs`; `@RestController` em `controller.<feature>`; `@StandardErrorApiResponses` em todos os `*API`; `GlobalExceptionHandler` em `web`.
- **Aplicação (primeira onda):** casos de uso em `application.<feature>` com interfaces `UseCase` / `VoidUseCase`, muitas operações ainda **delegando** a `*Service`; `*ResponseMapper` onde extraído; controllers finos; mensagens de erro em português/minúsculas nos fluxos alinhados; `SecurityContextUsers`, `TenantContext`, testes de controller (`WebMvcControllerTestImports`, `@MockitoBean` em filtros quando aplicável).

**Isto substitui** o antigo encadeamento “Passo 0 → … → Passo 4” para este repositório: **não** voltar a abrir PRs só para refazer baseline, piloto de cotação isolado ou mover `*API` para `controller.*.docs`.

---

## Visão em sequência (a partir da Fase 1)

```mermaid
sequenceDiagram
    participant F0 as Fase 0 Baseline feito
    participant F1 as Fase 1 Orquestração no use case
    participant F2 as Fase 2 Pacotes e mappers 06
    participant F3 as Fase 3 Integrações
    participant F4 as Fase 4 Clock
    participant F5 as Fase 5 ADR decisões

    Note over F0: não reexecutar
    F0->>F1: absorver lógica do *Service
    F1->>F2: opcional por feature
    F2->>F3: quando surgir integração clara
    F1->>F4: quando teste exigir tempo fixo
    F2->>F5: quando padrão global mudar
    F3->>F5: quando padrão global mudar
    F4->>F5: quando padrão global mudar
    F5->>F1: prioridades revisadas
```

---

## Fase 1 — Orquestração no caso de uso (absorver `*Service`)

**Objetivo:** A implementação `@Service` do **caso de uso** passa a conter a **orquestração** (repositórios, políticas de fluxo, chamadas a outros use cases); o `*Service` monolítico encolhe ou desaparece por operação — alinhado ao **06** (use case linear; sem regra de negócio pesada no controller) e ao **05** §4.4.

**Inclui (checklist por PR / por operação):**

- [ ] Escolher um método ou um conjunto coeso num `*Service` (evitar auth completo num único PR).
- [ ] Mover orquestração para `Xxx implements XxxUseCase` (ou criar use case se ainda for só delegação de uma linha **com** plano de absorção no mesmo PR ou no seguinte).
- [ ] Manter DTOs HTTP e contrato REST; ajustar apenas o wiring (controller → use case).
- [ ] `@Transactional` só onde for estritamente necessário (**05** §4.3); justificar no PR se mantiver/adicionar.
- [ ] `mvn test` verde.

**Pronto quando:** O fluxo tocado não depende do `*Service` para essa operação (ou o serviço ficou só como fachada mínima documentada até remoção).

**Depois:** Continuar Fase 1 noutra operação/agregado **ou** iniciar Fase 2 numa feature onde o pacote `application.<feature>` ficou grande.

**Evitar:** Dois beans como fonte de verdade para a mesma operação (`CreateXxx` + `XxxService.create` sem plano de remoção).

---

## Fase 2 — Pacotes e mappers alinhados ao **06** (opcional por feature)

**Objetivo:** Aproximar a árvore de código do pacote-alvo do **06**: `application/usecases/{feature}/{action}/` com `{Action}UseCase`, `{Action}`, DTOs de entrada/saída da operação e, **quando existir integração ou mapeamento não trivial**, `InputMapper` / `OutputMapper` estáticos.

**Inclui (checklist):**

- [ ] Só entrar quando `application.<feature>` tiver **muitos** tipos ou várias operações em paralelo (equipa sente atrito de PR).
- [ ] Migrar **uma feature** de cada vez (Convenção B no **05** §5.2); atualizar imports; `mvn test` verde.
- [ ] Não obrigar rename de `dto.*` globais no mesmo PR (podem coexistir com DTOs colocados na pasta da operação).

**Pronto quando:** Pelo menos uma feature piloto está na estrutura `usecases/...` (ou decisão documentada no PR para adiar).

**Depois:** Fase 1 nas features recém-reorganizadas (se ainda houver lógica no serviço) **ou** Fase 3 se integrações externas forem o gargalo.

**Evitar:** Big-bang mover todas as features sem piloto.

---

## Fase 3 — Integrações explícitas

**Objetivo:** Outbound HTTP, filas, terceiros, etc. em `application/integrations/{service}/` com interface + implementação, como no **06** — em vez de lógica “escondida” em `service` genérico.

**Inclui (checklist):**

- [ ] Identificar um adaptador (ex.: e-mail, cliente REST).
- [ ] Introduzir interface + implementação; injetar no use case; testes com duplo de teste ou cliente fake quando fizer sentido.

**Pronto quando:** O use case não chama diretamente detalhes de framework de integração espalhados; contrato da porta está claro.

**Depois:** Fase 1 noutros fluxos que reutilizem a integração **ou** Fase 5 se a política de erros/retry for transversal.

---

## Fase 4 — Tempo (`Clock`) só onde a regra e os testes exigirem

**Objetivo:** Instantes determinísticos em testes (expiração, trial, convites, etc.).

**Inclui (checklist):**

- [ ] Introduzir `Clock` (ou bean de tempo) **só** no fluxo que vai ganhar teste com instante fixo.
- [ ] Substituir `Instant.now()` / `System.currentTimeMillis()` **na parte orquestrada** pelo relógio injetado.

**Pronto quando:** Testes do fluxo não dependem do relógio real.

**Depois:** Repetir apenas noutros fluxos com a mesma necessidade; **ADR** se `Clock` global virar regra.

**Evitar:** Injetar `Clock` em toda a base “por precaução”.

---

## Fase 5 — Decisões transversais (ADR + docs)

**Objetivo:** Quando algo vira **regra do projeto** (novo pacote raiz, política obrigatória de tempo, split domínio/JPA em larga escala), ficar explícito.

**Inclui (checklist):**

- [ ] Novo **ADR** + atualização pontual de `02` / `05` / `06` conforme o caso.
- [ ] Itens grandes que **não** entram sem ADR (já fora do roadmap operacional): separação JPA vs entidade de domínio em **toda** a base; reescrita completa de `AuthService` / segurança; rename em massa de pacotes.

**Pronto quando:** Leitor novo sabe o que mudou e porquê.

**Depois:** Voltar à **Fase 1** (ou 2) com prioridades revisadas.

---

## Referência rápida: “terminei a fase N, e agora?”

| Fase concluída | Próxima ação típica |
|----------------|---------------------|
| 0 | **Não aplicável** — baseline já feito neste repo. |
| 1 (um PR) | Continuar **Fase 1** noutra operação **ou** **Fase 2** se a feature estiver madura para reorganizar pacotes. |
| 2 | **Fase 1** nas operações da feature reorganizada **ou** **Fase 3** se integrações forem o foco. |
| 3 | **Fase 1** em consumidores **ou** **Fase 5** se houver decisão global. |
| 4 | **Fase 1** ou **5** conforme contexto. |
| 5 | **Fase 1** (prioridade revisada). |

---

## Repositório novo (“greenfield”) neste mono/modelo

Se um módulo **não** tiver o histórico deste repo: antes da Fase 1, garantir **Fase 0 mínima** (docs de arquitetura, `mvn test` + CI, primeiro fluxo com `*API` + controller + um use case). O detalhe equivale ao antigo “Passo 0–1 + piloto” descrito no **06** e no **05**.

---

## Histórico de entregas (referência — não checklist)

*(Consolidado de entregas anteriores; mantém contexto para PRs e auditoria.)*

- Cotações, clientes, agência, lançamentos financeiros, convites, solicitação (pública + agência), termos, utilizadores, painel vendedor, auth: padrão `*API` + controller + `application.<feature>` + mappers onde aplicável, muitas rotas ainda com use case a delegar em `*Service`.
- Erros globais, tenant, OpenAPI `ApiError`, `@WebMvcTest` alinhados aos controllers.

Atualize esta lista **só** quando uma entrega mudar o baseline (ex.: “`CustomerService` removido por completo”) — não é obrigação a cada PR da Fase 1.
