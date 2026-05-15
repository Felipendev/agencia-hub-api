# Architecture Reference — Clean Architecture (Use Case Driven)

Este documento é a **referência detalhada** do modelo alvo (camadas, pacotes, regras e pirâmide de testes). Complementa **`05-package-refactoring-and-class-responsibilities.md`** (convenções incrementais, `@Transactional`, anti-padrões) e **`02-backend-architecture.md`** (visão geral). O código actual pode ainda divergir: a secção *Mapeamento para `agencia-hub-api`* no fim liga conceitos a pastas reais hoje.

---

## Layer Overview

```
HTTP Request
     │
     ▼
┌─ PRESENTATION ─────────────────────────────────────────┐
│  Controller (@RestController)                          │
│  API Interface (Swagger docs)                          │
│  RequestDTO (@Valid, Bean Validation)                   │
│  ResponseDTO                                           │
└────────────────────────────────────────────────────────┘
     │ calls UseCase.execute(RequestDTO)
     ▼
┌─ APPLICATION (Use Cases) ──────────────────────────────┐
│  UseCase (interface extends UseCase<I, O>)             │
│  UseCaseImpl (@Service, orchestrates only)             │
│  InputMapper (static, DTO → integration request)       │
│  OutputMapper (static, domain+integration → response)   │
└────────────────────────────────────────────────────────┘
     │ depends on                    │ depends on
     ▼                               ▼
┌─ DOMAIN ───────────────┐  ┌─ INTEGRATION ─────────────┐
│  Entity (business rules)│  │  IntegrationInterface     │
│  ValueObject            │  │  IntegrationImpl          │
│  Repository (interface) │  │  RestGenericClient        │
│  Enums                  │  │  Request/Response DTOs    │
│  NO framework deps      │  └─────────────────────────┘
└─────────────────────────┘
     ▲ implemented by
     │
┌─ PERSISTENCE ──────────────────────────────────────────┐
│  PostgresRepository implements DomainRepository        │
│  JPA Entity (@Entity, database mapping)                │
│  EntityMapper (toEntity / toDomain)                    │
│  Spring Data Repository (JpaRepository interface)      │
└────────────────────────────────────────────────────────┘
```

**Nota de mapeamento conceptual → este repositório:** a camada *Presentation* inclui o contrato OpenAPI; em Java, as interfaces `*API` vivem em `com.agenciahub.api.application.controller.doc` e os `@RestController` em `com.agenciahub.api.controller.<feature>` (ver **ADR 0004**).

---

## Package Structure (alvo)

Raiz do módulo: `com.agenciahub.api`.

```
com.agenciahub.api/
├── application/
│   ├── controllers/
│   │   └── docs/                    # Swagger API interfaces (*API, meta-anotações OpenAPI)
│   ├── usecases/{feature}/{action}/
│   │   ├── {Action}UseCase.java     # interface extends UseCase<I, O>
│   │   ├── {Action}.java            # @Service implementation
│   │   ├── {Action}RequestDTO.java  # input with @NotBlank etc.
│   │   ├── {Action}ResponseDTO.java # output
│   │   ├── InputMapper.java         # optional, static utility
│   │   └── OutputMapper.java        # static utility
│   ├── integrations/{service}/
│   │   ├── I{Service}Integration.java
│   │   ├── {Service}Integration.java
│   │   └── dto/request/ + dto/response/
│   └── persistence/{entity}/
│       ├── {Entity}Entity.java      # JPA @Entity
│       ├── {Entity}Mapper.java      # toEntity() / toDomain()
│       ├── {Entity}Repository.java  # Spring Data JPA interface
│       └── Postgres{Entity}Repository.java  # implements DomainRepository
├── domain/
│   ├── entities/{entity}/
│   │   ├── {Entity}.java           # rich domain entity
│   │   ├── {Entity}Validator.java
│   │   └── {Entity}ValueObject.java
│   ├── enums/
│   ├── repository/                  # interfaces only
│   └── valueobject/
├── common/
│   ├── configuration/
│   ├── security/
│   ├── utility/
│   └── messages/
└── token/                           # JWT auth filter, providers
```

**Convenção B “flat por feature”** (alternativa já documentada no **05**): `application.<feature>/` com `CreateXxx`, `CreateXxxUseCase`, etc., quando a pasta por operação ainda não for necessária.

---

## Rules

### Controller

- Thin: receives request, calls use case, returns response.
- Uses `@Valid` for input validation.
- No business logic.
- Implements API interface (Swagger separation).

### Use Case

- Interface: `UseCase<Input, Output>` with single method `execute(Input)`.
- Implementation: `@Service`, orchestrates domain + integration only.
- Does **not** contain business rules (delegates to domain entities).
- Does **not** use `@Transactional` unless technically justified (same policy as **05** §4.3).
- Linear flow: validate → fetch → execute domain logic → persist → return.

### Mappers

- **InputMapper:** use case DTO → integration request DTO.
- **OutputMapper:** domain entity + integration response → use case response DTO.
- Both are static utility classes with `@NoArgsConstructor(access = PRIVATE)` (or equivalent).
- Use null-safe collection checks where appropriate (project utility or explicit guards).

### Domain Entity

- Contains **all** business rules (validation, state transitions).
- Factory methods: `create()` for new, `rebuild()` for hydration from DB.
- Guard clauses for state validation (e.g. `isNotActive("message")`).
- No framework annotations (pure Java).
- Self-validates via Validator pattern where applicable.

### Repository

- Domain layer defines interface (e.g. `DomainCustomerRepository`).
- Persistence layer implements it (e.g. `PostgresCustomerRepository`).
- Implementation uses Spring Data JPA repository + `EntityMapper`.
- Save method: `findById` → merge existing **or** create new entity.

### Integration

- Interface in integration package.
- Implementation uses HTTP client abstraction (e.g. `RestGenericClient`) for outbound calls.
- Errors from remote services propagate naturally (no masking) unless a bounded translation is agreed.

### Testing Pyramid

- **Unit tests:** domain entities (fast, no Spring context).
- **Integration tests:** use cases (`@SpringBootTest` + Testcontainers when DB is required).
- **E2E tests:** controllers (`MockMvc` + `@AutoConfigureMockMvc`).
- **E2E:** at most ~2 tests per endpoint slice (success + primary error) unless the team agrees otherwise.
- **Integration:** typically 3–4 tests (success + error scenarios).
- **Unit:** comprehensive for business rules.

### Conventions

- No wildcard imports (`import java.util.*` → explicit imports).
- No unused imports.
- No methods created only for tests in production code.
- Bean Validation on DTOs, not manual checks duplicated in use cases when the same rule belongs on the DTO/domain.
- Prefer database constraints for uniqueness; surface violations via `GlobalExceptionHandler` (and a dedicated message registry if the project introduces one).
- Error messages in **lowercase Portuguese** for API-facing text (see **02** and **ADR 0003**).
- In tests: use secure random / factories for passwords and secrets — never hardcoded production-like credentials.

---

## Mapeamento para `agencia-hub-api` (hoje vs alvo)

| Alvo (este documento) | Estado típico no código hoje |
|------------------------|------------------------------|
| `application/controllers/docs` | **Adotado:** `*API`, `StandardErrorApiResponses`. |
| Controllers | **Adotado:** `application.controller` + `application.controller.doc` (**ADR 0007**). |
| `application/usecases/{feature}/{action}/` | **Adotado** para features HTTP (**Fase 2** / **ADR 0008**). |
| DTO Request/Response por operação | `application.usecases.<feature>.<ação>` (**ADR 0008**). |
| `InputMapper` / `OutputMapper` estáticos | **Piloto:** `customer` (**ADR 0009**); restantes features ainda com `*ResponseMapper` `@Component`. |
| `domain/` rico | **Parcial:** enums/tipos em `domain/`; regras ainda em use cases + entidades JPA. |
| `application/persistence/` | **Adotado:** `application.persistence.entity` + `.repository` (**ADR 0009**). |
| `application/integrations/` | **Adotado:** e-mail, verificação (**ADR 0005**, **0006**). |
| `common/`, `token/` | **Hoje:** `config`, `security`, `web`, `exception`, etc.; renomear pacotes só com passo explícito no roadmap. |

---

## Relação com outros documentos

| Documento | Função |
|-----------|--------|
| `02-backend-architecture.md` | Camadas, contratos `UseCase` / `VoidUseCase`, HTTP, OpenAPI. |
| `05-package-refactoring-and-class-responsibilities.md` | Pacotes evolutivos, `@Transactional`, anti-padrões, checklist de PR. |
| **Este `06-…`** | Referência **detalhada** Clean Architecture / use case driven e pirâmide de testes. |
| `04-incremental-refactoring-roadmap.md` | **Fases 1–4** operacionais; Fase 0 = baseline já feito neste repo (não repetir). |
| ADR 0004 | Por que `controller` e `application` são irmãos no pacote Java. |
