# F07 — integração e publicação de agenda e atas

PRs [API #65](https://github.com/Felipendev/agencia-hub-api/pull/65) e [UI #58](https://github.com/Felipendev/agencia-hub/pull/58), implementação Claude e revisão/integração Codex em 20/10/2026. Publicação ainda em validação; não confundir prévia com produção.

## Validação

26 testes backend distintos aprovados: AgendaTest (6), AgendaIntegrationTest (4), AgendaLifecycleIntegrationTest (4), AgendaMigrationUpgradeTest (1), DataDeletionIntegrationTest (8), DataDeletionMultiTenancyIntegrationTest (2), AgencyDeletionSchedulerIntegrationTest (1). O teste de upgrade V58→V59 foi repetido e aprovado após corrigir a fixture de pessoa, sem alterar a migration. PostgreSQL efêmero e contexto Spring real.

Frontend: 127 testes aprovados; build/tipos aprovados; lint sem erros, dois avisos preexistentes de cotações. Visual local da integração em 1440×1000 e 390×844 com API interceptada e dados fictícios, sem overflow global. Atalho do perfil de Milhas abriu a agenda com os parâmetros de pessoa e produto corretos.

Heads revisados: API `3976ec26ebeda49d0df452d55aed1b995de64a7b`, UI `4293dda5b289bbc9859e1f2245cefdb4a02a9b11`. CI build dos dois aprovada; Vercel preview aprovado. Railway prévia #65 deploy `845c1341-a9be-4b81-b0a5-25cd4c1b4adb` construiu a imagem, mas falhou em execução com UnknownHostException de postgres-ado4.railway.internal. O Postgres dessa prévia não tem implantação; o status SUCCESS do Railway não comprova saúde. Sem merge/publicação.

## Resultado da revisão

Navegação Principal dono/vendedor, proteção de /agenda e subrotas, atalhos da pessoa com TRAVEL/MILES. CRUD por ID, estados, fuso, atas e versões preservados. Reenvio com chave compara todos os campos normalizados; mudança de estado confere versão antes de no-op. Eventos de compromissos excluídos entram no bloqueio de exclusão/anonimização parcial da pessoa. Descarte autorizado da agência remove eventos/atas/versões/compromissos antes das referências. Não cria tarefas nem sincroniza agendas externas.

V59 preservada sem renumerar/editar histórico publicado. Após API em produção com migração/saúde confirmadas, publicar UI. Depois atualizar F01 e validar V59→V60; por último simulações por cliente V60→V61. V62 reservada para extensão F10 do Claude, após V61. Percentual de cobertura não medido; sem alegação de CRUD autenticado em produção.

## Bloqueio da prévia e ação concreta

Ambiente **agencia-hub-api-pr-65**, ID f5fa4814-5dac-4aab-bdae-3197441e262a; serviço **Postgres**, ID d84006aa-dcc8-4e24-bdeb-a881298c0da1. Fonte e volume já existem, mas nenhuma implantação do banco foi criada. Reconectar a mesma imagem não gerou mudança. Foi preparada uma única alteração nesse ambiente: deploy.sleepApplication=false. O conector confirmou um campo não destrutivo, sem mudanças em produção, mas accept-deploy recusou: “Cancelled — the user did not approve this action. No changes were made.”

No painel Railway, abrir exatamente essa prévia e o serviço Postgres, revisar a alteração Sleep Application=false e clicar Deploy Changes. Não editar DB_HOST/senhas/schema, não remover volume/banco e não aplicar à produção. Após o banco iniciar, Codex valida a API da prévia e só então integra/publica API antes da UI. A prévia deve passar primeiro; não contornar com merge direto em produção.

Produção conferida durante o bloqueio: site e OpenAPI HTTP 200, ainda na versão anterior. A revisão adicional desativa repair automático por padrão, mantendo a validação de checksums. Testes finais com inicialização de schema isolado em execução.
