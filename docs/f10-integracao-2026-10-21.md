# F10 — revisão e integração API

Revisão Codex de API #66, implementação Claude, em 21/10/2026. V62 é a única migration nova; V1–V61 preservadas. Publicação exige CI e prévia do head final, seguida de saúde em produção antes da UI.

## Correções da revisão

- Reenvio de criação confere todos os campos normalizados, inclusive fornecedor, pessoas, notas, local, confirmação, fim e fuso.
- Mudança de status valida expectedVersion antes de devolver sucesso para o mesmo estado.
- Reagendamento preserva fuso anterior junto com início/fim anteriores.
- Descarte autorizado da agência remove eventos, vínculos e etapas antes de viagens, preservando outras agências.
- Auditoria inclui customerId no JSON de todos os eventos para manter rastreabilidade após excluir ou reatribuir viagem. Nenhuma migration adicional foi criada.

## Privacidade e integridade

A rotina LGPD existente anonimiza contatos; ela não exclui viagens automaticamente. Etapas, textos e auditoria são dados que exigem revisão: a solicitação permanece PENDING, com mensagem específica, sem falsa conclusão ou alteração parcial. A regra também considera eventos de viagens já excluídas. Exclusão comum de pessoa com esse histórico recebe 409 TRIP_STAGE_HISTORY_EXISTS; exclusão da viagem remove etapas/vínculos por cascata e conserva auditoria. A política futura de retenção/anonimização integral continua pendente, como nos módulos de contratos, economia e agenda.

## Validação

46 testes aprovados: TripStagesTest 4, TripStagesIntegrationTest 4, TripStagesLifecycleIntegrationTest 4, TripStagesMigrationUpgradeTest 1, DataDeletionIntegrationTest 8, DataDeletionMultiTenancyIntegrationTest 2, AgencyDeletionSchedulerIntegrationTest 1, AgendaLifecycleIntegrationTest 4, WorkIntegrationTest 10, SimulationCustomerIntegrationTest 8. PostgreSQL efêmero, Spring/HTTP reais. Testado banco novo e upgrade V61→V62 com preservação da viagem e ausência de preenchimento automático.

Gate scripts/check-migrations.sh origin/main aprovado. Revisão manual de validação, SQL parametrizado, permissões, isolamento, idempotência, concorrência, dados pessoais e ausência de efeitos financeiros concluída. JaCoCo: TripStageService 155 linhas cobertas/7 não cobertas; TripStages 42/1; TripStageHistory 2/0. Cobertura de branches do serviço 87/120; não se alega cobertura integral.

Evidências locais: f10-final-api-tests.log e target/surefire-reports (artefatos ignorados pelo Git). API testada localmente com PostgreSQL embarcado 14.15; a prévia Railway usa PostgreSQL 18, com inicialização a conferir no head revisado. Não houve teste autenticado com dados reais em produção.
