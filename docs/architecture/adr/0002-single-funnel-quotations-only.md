# ADR 0002 — Funil único: apenas cotações (remoção de opportunities)

## Status

Aceito — implementado no código; ambientes existentes aplicam `V17__remove_opportunities.sql` (ou recriam o schema).

## Contexto

O produto expunha dois conceitos sobrepostos: **opportunities** (`/opportunities`, “atendimento” no front) e **quotations** (`/quotations`, “cotação”). Não havia dados de produção; o time decidiu que **cotação** cobre o funil inteiro.

## Decisão

- Remover a entidade **Opportunity**, o repositório, serviços, controller e DTOs associados.
- Remover `opportunity_id` de **quotations** e a tabela `opportunities` (migração `V17`).
- Manter **Quotation** como único agregado de proposta/funil no backend.

## Consequências

- O front não deve mais chamar `/opportunities`; leads e rascunhos passam por `/quotations` (ex.: status `DRAFT`).
- `CreateQuotationRequest` / `UpdateQuotationRequest` / `QuotationResponse` **não** expõem `opportunityId` nem título de oportunidade.
- Histórico Flyway até `V16` ainda cria `opportunities` em instalações antigas; `V17` remove tudo relacionado.

## Alternativas não escolhidas

- Manter opportunities como “lead leve” antes da cotação — rejeitado por redundância de produto.
- Renomear apenas na API — insuficiente; o modelo de dados continuaria duplicado.
