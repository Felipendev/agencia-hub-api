# Project Configuration
# Only contains overrides. Properties not listed use framework defaults.

## Backend Conventions

# architecture.pattern: Clean Architecture — igual ao default do framework, sem
# override necessário. Confirmado como o padrão já em uso no código:
# domain/, application/usecases/<domínio>/<ação>/ (CreateX + CreateXUseCase +
# RequestDTO + InputMapper), infrastructure/, web/.

## Quality Gates

coverage:
  min_coverage: 90            # Override: time exige 90% em vez do default de 80%

## Team Conventions

language:
  specs: pt                   # Override: specs em português (consistente com o hub em ../milhas-hub)

## Contexto

Este repo é um dos alvos de implementação da área de **milhas** do agencia-hub —
ver o direcionador em `../milhas-hub/SDD-DIRECIONADOR.md` e o workspace de
planejamento em `../milhas-hub/sdd/PROJECT.md`. Milhas não é um serviço separado;
é uma faceta de `customers` implementada aqui.
