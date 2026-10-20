# F01 — checklists e rotinas

Implementação em `codex/f01-checklists` nos dois repositórios. **Ainda não publicada:** V60 reservada para este módulo; V59 pertence à agenda/atas do Claude e precisa entrar primeiro. A implementação não cria uma V59 vazia, não renumera V60 nem desativa o gate de migrations.

## Entrega

- Tarefas avulsas por pessoa e produto TRAVEL/MILES: criar, consultar, editar pelo mesmo ID e excluir logicamente enquanto abertas. Concluir/cancelar/reabrir são operações explícitas, com versão, autor, data e motivo quando necessário. Encerradas precisam reabrir antes de editar/excluir.
- Modelos: CRUD, etapas editáveis (até 50), prazo relativo e frequência checklist inicial/semanal/mensal. Produto imutável. Exclusão lógica bloqueia aplicações futuras, preservando execuções.
- Execuções: aplicar modelo por pessoa/período, consultar sua cópia original, editar título/observações pelo mesmo ID e cancelar. Cancelamento substitui exclusão para preservar histórico; afeta somente tarefas abertas. Pessoa, produto, período e snapshot são preservados.
- Geração explícita, sem agendamento automático. Semana começa segunda-feira; mês segue calendário. Reaplicar modelo/pessoa/período retorna a mesma execução mesmo após editar modelo ou cancelar execução. Para ONCE, existe uma execução por modelo/pessoa.
- Filtros por pessoa, responsável, situação, atraso e execução; menus separados em Viagens e Milhas e atalhos nas respectivas fichas. IDs do CRM são compartilhados, sem duplicar pessoas/carteiras.
- Responsável ativo e referências à cotação/viagem validados pela agência/pessoa. Vínculos de viagens exclusivos de TRAVEL. Sem efeitos em vendas, financeiro ou saldo de milhas.

## Contrato para integrações futuras

Base autenticada `/work`, papéis AGENCY_OWNER e SALES_AGENT. Agência e autor sempre da sessão.

- `GET /members`, `GET /references?customerId=...` (opções da mesma pessoa/agência).
- `GET /tasks` com product/customerId/responsibleId/state/dueBefore/runId; `GET /tasks/{id}`.
- `POST /tasks` exige UUID em `Idempotency-Key`; body product, customerId, title, notes, dueOn (data civil), responsibleId/tripId/quotationId opcionais e expectedVersion=0. Mesma chave/payload retorna o registro original; outra carga com a chave é conflito.
- `PATCH /tasks/{id}` usa os mesmos campos e versão atual. `DELETE /tasks/{id}?expectedVersion=N` registra exclusão lógica. `POST /tasks/{id}/state` recebe state, reason e expectedVersion.
- `GET/POST /templates`, `PATCH/DELETE /templates/{id}` com versão; `POST /templates/{id}/apply` recebe customerId, date e responsibleId opcional.
- `GET /runs`, `GET/PATCH /runs/{id}`, `POST /runs/{id}/cancel` com motivo e versão. Não há DELETE: cancelamento mantém histórico. Atualização altera somente título/observações.
- `GET /history/{TASK|RUN|TEMPLATE}/{id}` consulta os retratos anteriores, inclusive exclusões lógicas, dentro da agência.

Modelos/execuções são o caminho inicial para carteira/agenda. Não inventar um endpoint de origem/tarefas já integrado: os consumidores serão implementados em PRs posteriores usando este contrato real e uma chave de confirmação persistida. ID de cliente/usuário da requisição nunca substitui validação de tenant.

## Integridade e privacidade

Locks transacionais e chave única impedem duplicação concorrente de aplicação; versão controla edições/transições/exclusões. Aplicar/cancelar usa inserção/atualização e auditoria em lote, sem consulta por etapa.

Tarefas/histórico bloqueiam exclusão silenciosa da pessoa e anonimização parcial, com mensagem de revisão pendente. Inativar a pessoa não apaga histórico. Descarte autorizado da agência limpa eventos/tarefas/execuções/modelos antes dos demais vínculos e mantém outras agências intactas. Uma política de retenção/anonimização automatizada de texto livre não foi criada nesta entrega.

## Validação e publicação

Resultados locais em 20/10: 29 testes backend distintos aprovados (28 no conjunto de regressão/upgrade e, após acrescentar a referência de Viagens, os 10 de WorkIntegrationTest repetidos e aprovados); 122 testes frontend aprovados, build/tipos aprovados e lint com zero erros e duas advertências preexistentes de cotações. Não são testes autenticados em produção.

Comandos: `mvn test -Dtest=WorkIntegrationTest,WorkMigrationUpgradeTest,DataDeletionIntegrationTest,DataDeletionMultiTenancyIntegrationTest,AgencyDeletionSchedulerIntegrationTest,TripSavingsIntegrationTest`; validação final da referência: `mvn test -Dtest=WorkIntegrationTest`; frontend `npm test`, `npm run lint`, `npm run build`.

Testes HTTP com PostgreSQL efêmero: CRUD, versão, auditoria, papéis/tenant, concorrência, snapshots, períodos, cancelamento/reabertura, privacidade, descarte por agência e referências de Viagens. Regressões de privacidade/economia incluídas. Teste de upgrade local parte da V58, preserva registro existente e aplica V60; **não substitui o teste V59 → V60**, ainda pendente da entrega Claude.

Frontend: testes de edição pelo mesmo ID, confirmação após API, preservação de formulário em erro, chave idempotente em reenvio, aplicação/modelos/execuções, exclusão recusada e separação de produto. Build e lint sem erros novos; duas diretivas ESLint antigas em cotações permanecem fora de escopo. Evidências visuais com dados fictícios interceptados apenas no navegador local, sem chamada de negócio à API real. Desktop 1440×1000 e celular 390×844 sem rolagem horizontal global.

Antes de publicar: integrar V59, atualizar esta branch com main, rodar teste do upgrade real e regressões, confirmar CI incluindo gate sequencial, validar prévia própria e só então publicar API e UI. Não promover a prévia de UI contra uma API de produção que ainda não possui `/work`.
