# Refatoração de pacotes e responsabilidades de classes

Este documento **define** o modelo alvo de pacotes e o papel de cada tipo de classe no backend `agencia-hub-api`, para orientar refatorações **sem** ambiguidade entre “use case” e “service”. Complementa `02-backend-architecture.md`, o roadmap `04-incremental-refactoring-roadmap.md` e o **ADR 0004** (mapeamento pacotes Java ↔ camadas conceptuais). Para a visão **Clean Architecture / use case driven** (diagrama de camadas, `InputMapper`/`OutputMapper`, persistência vs domínio, pirâmide de testes), ver **`06-clean-architecture-use-case-driven.md`**.

---

## 1. Objetivo

- Tornar **explícito** onde vive cada responsabilidade (HTTP, orquestração de operação, persistência, mapeamento).
- Permitir **evolução incremental**: não exige big-bang nem cópia literal de estruturas de outros repositórios de referência.
- Reduzir o padrão “**use case fino + `*Service` com toda a lógica**” quando o time optar por refatorar uma feature.

---

## 2. Estado baseline (aceite como legítimo até migrar)

| Pacote (raiz) | Papel hoje |
|----------------|------------|
| `com.agenciahub.api.controller.<feature>` | `@RestController` (implementa `*API`). |
| `com.agenciahub.api.application.controllers.docs` | Interfaces `*API` + OpenAPI (`StandardErrorApiResponses`, etc.). |
| `com.agenciahub.api.application.<feature>` | Interfaces `*UseCase`, implementações `@Service`, comandos/consultas, `*ResponseMapper`. |
| `com.agenciahub.api.service` | Fachadas transacionais com repositórios JPA, muitas operações por agregado (`QuotationService`, `CustomerService`, …). |
| `com.agenciahub.api.repository` / `entity` | Persistência JPA. |
| `com.agenciahub.api.web` | `GlobalExceptionHandler` e cross-cutting HTTP de erros. |

**Regra:** código neste formato **não está “errado”** por existir; só deixa de ser o alvo quando uma feature for **deliberadamente** migrada para o modelo da secção 4.

---

## 3. Camadas conceptuais (responsabilidade, não nome de pasta)

| Camada | Responsabilidade | O que **não** deve fazer |
|--------|-------------------|-------------------------|
| **API** | Contrato HTTP, validação de forma (`@Valid`), tradução para comando/consulta, resposta e documentação OpenAPI. | Regra de negócio pesada, queries JPA, orquestração longa. |
| **Application (caso de uso)** | **Uma operação de aplicação**: orquestra repositórios, portas, integrações, políticas do fluxo (ex.: “vendedor sem `sellerId` no body”). **Transação explícita só quando for estritamente necessário** (ver secção 4.3). | Ignorar erros de domínio; duplicar validação que o domínio já garante (quando existir domínio rico). |
| **Domain** | Invariantes, estados, políticas de negócio **independentes** de Spring/JPA (quando extraídas). | Dependência direta de `HttpServletRequest`, repositório concreto. |
| **Infrastructure** | Adaptadores (e-mail, clientes HTTP, etc.), implementações de portas — **evolução futura** conforme ADRs. | Regras que pertencem ao núcleo do negócio sem ser adaptação. |

---

## 4. Modelo alvo de classes (por operação)

### 4.1 Princípio central

**Uma operação exposta ao mundo (criar cliente, sincronizar utilizador, etc.) deve ter um único ponto de orquestração claro:** a **implementação do caso de uso** (classe `@Service` que implementa `XxxUseCase`).

- Essa classe pode injetar **repositórios**, **integrações**, **mappers**, **outros use cases** pequenos.
- **Não** é obrigatório existir um segundo tipo `XxxService` com os mesmos métodos só para “ter uma camada service”.

### 4.2 Papel de cada tipo de classe

| Tipo | Pacote sugerido (ver secção 5) | Responsabilidade |
|------|--------------------------------|------------------|
| **`*API`** | `application.controllers.docs` | Contrato REST + OpenAPI; sem lógica de negócio. |
| **`*Controller`** | `controller.<feature>` | Implementa `*API`; delega a `*UseCase`; liga `@AuthenticationPrincipal`, headers, `@Valid`. |
| **`*UseCase` (interface)** | `application…` | Contrato da operação (`execute(Input)` ou `void`). |
| **Implementação do use case** (`@Service`, nome = verbo/ação) | `application…` | **Todo** o fluxo da operação que hoje estaria “escondido” num `*Service` monolítico **desta** operação. |
| **Comando / consulta (record ou tipo dedicado)** | Junto do use case ou subpacote `…commands` / `…queries` | Entrada imutável da operação (evitar “god parameter list”). |
| **DTO de API** | `dto.<feature>` (ou colocalizado se o time adotar pacotes por operação) | Forma do JSON; validação Bean Validation. |
| **`*ResponseMapper`** | `application.<feature>` | Conversão entidade → DTO de resposta (sem regra de negócio pesada). |
| **Repositório Spring Data** | `repository` (até existir ADR de ports em `infrastructure`) | Acesso a dados; queries específicas. |
| **`GlobalExceptionHandler`** | `web` | Tradução de exceções para `ApiError` + HTTP. |

### 4.3 Transações (`@Transactional`)

**Regra geral:** **evitar** `@Transactional`. Não é padrão obrigatório em use cases nem em serviços novos. Muitas operações ficam corretas com o comportamento **implícito** do Spring Data / uma única chamada ao repositório.

**Usar `@Transactional` apenas quando for estritamente necessário**, por exemplo:

- Várias escritas que **têm** de commitar ou falhar **em conjunto** (atomicidade real entre entidades ou passos).
- Propagação / isolamento específicos (`REQUIRES_NEW`, etc.) exigidos pelo fluxo.
- Leitura que **sem** transação `readOnly` causa problema mensurável (ex.: pressão em conexão, lazy loading controlado) — caso raro; justificar.

**Evitar:**

- `@Transactional` “por precaução” em cada método de use case ou leitura.
- `@Transactional` no controller.
- Duplicar anotações em cadeia (controller + use case + service) para a mesma operação.

**Em PR:** se introduzir ou manter `@Transactional`, incluir **uma linha** no texto do PR a explicar **por que** é indispensável nesse ponto.

### 4.4 O que acontece ao antigo `*Service` por agregado

| Situação | Ação |
|----------|------|
| Método `create` / `update` migrado integralmente para `CreateXxx` / `UpdateXxx` | Remover o método do `*Service` ou o ficheiro inteiro se ficar vazio. |
| Lógica **partilhada** por várias operações do mesmo agregado | Extrair para **classe auxiliar** no mesmo pacote da feature (`XxxPolicy`, `XxxValidator`, `XxxPricing`) **sem** ser um segundo “god service”; ou manter temporariamente métodos **package-private** / helper até segunda passagem. |
| Operação ainda não migrada | O `*Service` continua a ser a fonte da verdade **até** o PR de migração dessa operação. |

---

## 5. Pacotes: duas convenções permitidas

### 5.1 Convenção A — “flat por feature” (atual, válida)

```
com.agenciahub.api.application.<feature>/
  CreateXxx.java
  CreateXxxUseCase.java
  CreateXxxCommand.java
  XxxResponseMapper.java
```

- **Quando usar:** menor atrito, features pequenas, equipa já habituada.
- **Critério:** até **~8–12** tipos públicos por pasta; acima disso, avaliar Convenção B.

### 5.2 Convenção B — “por operação” (opcional, recomendada para features grandes)

```
com.agenciahub.api.application.usecases.<feature>.<verbo>/
  CreateCustomer.java              # implements CreateCustomerUseCase
  CreateCustomerUseCase.java
  CreateCustomerCommand.java       # ou Request se preferirem o nome
  (opcional) CreateCustomerResponseDTO.java
```

- **`<verbo>`:** `create`, `update`, `delete`, `list`, `getbyid`, etc. (inglês, minúsculas, consistente com nomes de classes).
- **Quando usar:** muitas operações, DTOs e mappers por operação; paralelismo de PRs por pasta sem conflitos constantes.
- **Regra:** o **controller** continua em `controller.<feature>`; **não** mover `@RestController` para dentro de `application` (mantém-se alinhado ao **ADR 0004** e ao `02-backend-architecture.md`). As interfaces `*API` vivem em `application.controllers.docs`.

### 5.3 Transição A → B

- **Uma feature de cada vez** (ex.: `customer`), com `mvn test` verde.
- Atualizar imports e, se existir, `.cursor/rules` ou checklist interno do time.
- Não misturar num mesmo PR **reorganização de pastas** com **mudança de regra de negócio**.

---

## 6. Anti-padrões (evitar em novos códigos e remover em refactors)

1. **Use case** que só delega uma linha ao `*Service` **sem** plano de absorver a lógica — ou absorver no use case, ou documentar exceção temporária no PR.
2. **`*Service` público** com dezenas de métodos sem coesão — fatiar por operação ou por subdomínio documentado.
3. **Controller** com `if` de regra de negócio ou chamadas a repositório.
4. **Dois beans** diferentes (`CreateCustomer` + `CustomerService.create`) como “dupla fonte de verdade” para a mesma operação após migração incompleta.
5. **`@Transactional` sem necessidade** — anotação por hábito ou “para garantir”; preferir ausência e só adicionar com justificativa (secção 4.3).

---

## 7. Checklist de PR (refator por feature)

- [ ] Responsabilidade da operação está **numa** implementação de use case (ou justificativa explícita para exceção).
- [ ] **`@Transactional`:** ausente por omissão; se existir, **justificado** no PR como estritamente necessário (secção 4.3).
- [ ] Controller inalterado em contrato HTTP (salvo acordo explícito).
- [ ] `mvn test` verde.
- [ ] Atualizar **uma** linha no `04-incremental-refactoring-roadmap.md` (progresso) se a feature mudar de convenção A→B ou eliminar `*Service` relevante.

---

## 8. Relação com outros documentos

| Documento | Função |
|-----------|--------|
| `02-backend-architecture.md` | Camadas gerais, contratos `UseCase` / `VoidUseCase`, layout HTTP. |
| `04-incremental-refactoring-roadmap.md` | Ordem de trabalho e princípio “sem rename em massa”. |
| `06-clean-architecture-use-case-driven.md` | Referência detalhada: camadas, pacotes alvo, mappers, repositório domínio/persistência, pirâmide de testes. |
| ADR 0004 | Por que `controller` e `application` são irmãos no pacote Java. |
| Este `05-…` | **Definições** de pacotes evolutivos e **papel** de cada classe no refator. |

---

## 9. Resumo em uma frase

**Use case = orquestração da operação (pode ser o único `@Service` da operação); `*Service` monolítico = legado a fundir por feature; pacotes = Convenção A ou B por decisão de equipa, sempre incremental; `@Transactional` = só quando estritamente necessário, com justificativa.**
