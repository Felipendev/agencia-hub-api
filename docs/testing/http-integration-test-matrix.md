# Matriz de testes HTTP de integração

Convenção: para **cada endpoint** existem pelo menos **dois** testes em `src/test/java/com/agenciahub/api/integration/http/`:

| Cenário | Objetivo |
|---------|----------|
| **Feliz** | Autenticação válida (quando aplicável), corpo válido, `2xx` esperado |
| **Triste** | Sem auth (`401`), recurso inexistente (`404`), validação (`400`) ou regra de negócio |

Infraestrutura: `AbstractIntegrationTest` (PostgreSQL embarcado + Flyway + `MockMvc` com `/api/v1`).

## Cobertura por controller

| Controller | Classe de teste | Endpoints |
|------------|-----------------|-----------|
| Auth | `AuthHttpIntegrationTest` | login, invite, register (amostra) |
| Agency | `AgencyHttpIntegrationTest` | GET/PATCH `/agency` |
| Customer | `CustomerHttpIntegrationTest` | CRUD + lookup |
| User | `UserHttpIntegrationTest` | list, sales-agents, get, create, patch |
| Quotation | `QuotationHttpIntegrationTest` | list, get, create, patch, delete |
| Financial | `FinancialEntryHttpIntegrationTest` | list, get, create, patch |
| Invitation | `InvitationHttpIntegrationTest` | list, create, delete |
| Sales agent dashboard | `SalesAgentDashboardHttpIntegrationTest` | `/me`, `/{agentId}` |
| Solicitação | `SolicitacaoHttpIntegrationTest` | público + agência |
| Termos | `TermsHttpIntegrationTest` | público + accept |

Testes **slice** (`*WebMvcTest`) continuam em `application/controller/` com use cases mockados — mais rápidos para contrato JSON e validação.

## Credenciais de teste

- E-mail: `admin@agenciahub.com`
- Senha: `admin123` (seed Flyway `V7` + migrações de tenant `V9`)
