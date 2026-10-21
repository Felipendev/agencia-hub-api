# F09 — integração e validação com F10

Revisão Codex em 21/10/2026 de API #67 e UI #60, implementação Claude. Cadastro de indicações por produto; créditos, cupons, comissões, descontos, resgates e estornos continuam fora do escopo, aguardando regras de negócio.

## Integrações e correções

- Interface: /indicacoes e subrotas protegidas; menus de Viagens/Milhas para gestor e vendedor, com produto explícito e destaque correto; atalhos nas duas fichas da pessoa. Detalhe de Milhas aponta para a ficha de Milhas.
- Mudança de produto oculta os resultados anteriores durante o carregamento; troca de sessão/rota reinicia estado. Cadastro de prospecto conserva o ID criado se o vínculo falhar e permite repetir só o vínculo, sem nova pessoa.
- API: reenvio confere todos os campos normalizados. Status e vínculo exigem versão atual antes de no-op. Referências de pessoas são bloqueadas em ordem estável para serializar reenvios e coordenar exclusão.
- Resumo recusa período invertido. Exclusão de pessoa/usuário com vínculos tem mensagem específica; não sugerir que cancelar a indicação libera seu histórico. Descarte autorizado da agência remove indicações/eventos antes das referências.
- Auditoria conserva participantes e e-mail do contato para revisão de privacidade após edição ou exclusão. Solicitações ficam PENDING se ainda existem indicações/histórico, incluindo contato sem cadastro CRM. Dados de outra agência não autorizam processamento da solicitação. Não há promessa de anonimização integral; a política futura de retenção exige outra entrega.

## Validação sequencial local, sem publicar dependência

Produção ainda está em V61. Para testar sem integrar V63 antes de V62, foram criadas branches locais de validação, não publicadas e sem PR:

- API: codex/f09-validacao-v62, commit 415c5df4d05b74b0cd992af293f2470431d1498b, F09 revisado + F10 API e82a06ddab449a1855d0ac2d2c3473a73bf6846a.
- UI: codex/f09-ui-validacao-v62, commit 13bd532c4c424188f5fe30065411734ac6bef69b, F09 revisado + F10 UI 57fdf27cd94d885bd404beb9a6582f75f7ccf37b.

As branches dos PRs de F09 continuam independentes, baseadas na main V61. A resolução local dos três conflitos de API preservou ambos os módulos nos arquivos DeleteCustomer, ProcessDataDeletionRequest e GlobalExceptionHandler. Na integração final, atualizar com main após publicar V62 e repetir o gate/testes no head real, conservando as duas proteções. Não substituir arquivos centrais com apenas um dos módulos.

### API

61 testes distintos aprovados, zero falhas/erros/ignorados nos relatórios finais: ReferralsTest 4, ReferralsIntegrationTest 4, ReferralsLifecycleIntegrationTest 6, ReferralsMigrationUpgradeTest 1; TripStagesTest 4, TripStagesIntegrationTest 4, TripStagesLifecycleIntegrationTest 4, TripStagesMigrationUpgradeTest 1; privacidade 8, isolamento de privacidade 2, descarte da agência 1, ciclo Agenda 4, Work 10 e SimulationCustomer 8.

Spring/HTTP e PostgreSQL efêmero 14.15 reais. Banco novo e upgrade V62→V63 preservando viagem/etapa, sem preenchimento de indicações. Testes de idempotência, concorrência, versão, participantes atuais/anteriores, contato fora do CRM, isolamento e descarte. A primeira execução combinada apontou que o teste antigo V61→V62 aplicava também V63 ao migrar até latest; corrigido o alvo para 62 no PR #66, CI desse head aprovada. Os upgrades foram repetidos; o teste F09 também fixa seu alvo em 63 para futuras migrations. Nova regressão de contato de outra agência passou. Relatórios finais somam 61 testes aprovados; não se oculta a falha inicial do teste de upgrade.

Gate da árvore combinada aprovado; nenhuma migration publicada alterada. V62 e V63 são as únicas novas nessa árvore. O PR #67 sozinho deve continuar bloqueado pelo gate enquanto main não tiver V62; isso não é falha a ignorar.

JaCoCo: ReferralService 212 linhas cobertas/3 não cobertas, branches 112/166; Referrals 65/3, branches 91/127; ReferralHistory 3/0. Não se alega cobertura integral.

### Interface

161 testes aprovados na branch F09 antes do teste adicional dos atalhos de Milhas. Suíte combinada final: 173 testes em 45 arquivos, todos aprovados com maxWorkers=2, incluindo F10 e a nova regressão de atalhos. Um teste antigo de contratos excedeu o tempo na primeira execução paralela; passou isoladamente e na suíte completa repetida com menos processos. Nenhum teste foi removido ou enfraquecido.

Tipos aprovados. Lint zero erros, dois avisos preexistentes em cotações. Build local Webpack aprovado na repetição; a tentativa Turbopack e a primeira Webpack falharam no carregamento/parsing de fontes Google, sem mudança no produto para contornar. CI do frontend em bb7d43b e prévia Vercel aprovadas; confirmar novamente o head final com documentação/atalhos atualizados.

Revisão manual de SQL parametrizado, referências por agência, papéis, validação, histórico, ausência de efeitos financeiros e React/UX aprovada. Navegação desktop/celular verificada em 1440×1000 e 390×844, overflow global zero. Menu mudou de Milhas para Viagens com o filtro e o destaque corretos. Dados fictícios e fetch interceptado, sem chamada de negócio à API real. Capturas em docs/coordination/evidencias-f09-integracao/.

## Publicação e retomada

Nenhum merge/publicação realizado. Railway mantém novos builds pausados em todas as regiões, conforme https://status.railway.com/ (atualização consultada de 13:40 UTC / 10:40 Fortaleza). A prévia F10 ainda usa def6452, anterior à revisão; o status SUCCESS antigo não valida os novos heads. Produção preservada em V61.

Após normalização: validar prévia F10 no head e82a06d; publicar API #66/V62 e conferir migration/inicialização/saúde; publicar UI #59 e conferir Vercel READY. Atualizar branches F09 com main, preservar as resoluções locais descritas, repetir upgrade V62→V63/gate/CI, validar prévia F09 e publicar API #67 antes da UI #60. Não usar placeholders, repair, outOfOrder nem configurações de antigas prévias.
