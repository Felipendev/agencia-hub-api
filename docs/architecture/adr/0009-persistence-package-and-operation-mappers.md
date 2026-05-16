# ADR 0009 — Pacote `application.persistence` e mappers por operação

## Status

**Aceita** — fase 1 (move JPA) concluída; piloto `InputMapper`/`OutputMapper` em `customer`.

## Contexto

O doc **06** e o roadmap incremental previam:

- Entidades JPA e repositórios Spring Data fora da raiz `entity`/`repository`.
- Mappers estáticos `InputMapper` / `OutputMapper` por operação, em vez de `@Component` genéricos partilhados.

Até ADR 0008, persistência vivia em `com.agenciahub.api.entity` e `com.agenciahub.api.repository`.

## Decisão

### A. Persistência JPA

1. **Entidades JPA** em `com.agenciahub.api.application.persistence.entity`.
2. **Repositórios Spring Data** (e `spec/`) em `com.agenciahub.api.application.persistence.repository`.
3. Migrações Flyway **inalteradas** (nomes de tabela/coluna não mudam nesta fase).
4. **Domínio rico** separado do JPA (entidades de domínio + ports) fica **fora** deste ADR — só quando houver necessidade e ADR dedicado.

### B. Mappers por operação (piloto)

1. Feature **customer** como piloto:
   - `customer/shared/OutputMapper` — estático, `CrmCustomer` → `CustomerSummaryResponseDTO`.
   - `customer/create/InputMapper` — estático, `CreateCustomerRequestDTO` → `CrmCustomer` (novo).
2. Remover `CustomerResponseMapper` como bean Spring.
3. Replicar o padrão noutras features **incrementalmente** (não big-bang).

### C. Linguagem ubíqua (com ADR 0008)

- `usecases.user` → `usecases.platformaccount` (gestão de contas com login).
- `GET /users/sales-agents` (antes `/users/sellers`).
- Campo JSON `salesAgent` no painel (`SalesAgentDashboardResponseDTO`).

## Consequências

### Positivas

- Pacotes alinhados ao **06** sem reescrever o domínio de uma vez.
- Contrato de mapeamento visível na pasta da operação (piloto customer).

### Negativas

- Imports mudam em massa num PR (diff grande, baixo risco se testes verdes).
- Outras features ainda usam `*ResponseMapper` `@Component` até migração gradual.

## Relação

- **ADR 0008** — identidade `PlatformAccount` / `CrmCustomer`.
- **06** — referência de camadas; tabela “hoje vs alvo” actualizada após este ADR.
