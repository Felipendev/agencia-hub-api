# F08 — acompanhamento comercial

TODO-042, Codex, implementação iniciada em 27/10 e liberação em 28/10/2026. Mesmas pessoas do CRM, ações independentes para Viagens e Milhas. Não gera tarefas, viagens, mensagens automáticas, vendas, financeiro ou movimentos de milhas.

## Entrega

- Criar, consultar, editar/reprogramar pelo mesmo ID e excluir ações abertas com histórico preservado. Concluir, cancelar/reabrir com motivo e versões.
- Pessoa/produto imutáveis; responsável ativo da agência, data de Fortaleza, filtros de pessoa/responsável/situação/prazo e totais de abertas. Próximas inclui todas as datas posteriores a hoje.
- Idempotência compara payload completo, conflitos 409 preservam formulário. Histórico consultável para excluídas. Isolamento e permissões dono/vendedor; admin de plataforma bloqueado.
- Histórico impede exclusão direta de pessoa e conclusão indevida de anonimização. Descarte definitivo da agência respeita dependências. Sem promessa de anonimização parcial completa.

## API publicada

- PR #68, head b4fd0e79fda6760fdb6b1cc81db1f07cfeecab97; merge 33f8238ecc6e5d7caed30534d977d505a8aae865.
- V64 reservada no registro central antes do SQL. Não alterou migrations V56–V63. 70 testes JUnit reais aprovados, zero falhas/erros/skips, PostgreSQL efêmero/Spring/HTTP. Inclui 9 casos novos F08, banco novo e upgrade V63→V64 com CRM existente preservado; regressões F01/F07/F09/F10, simulações, privacidade e descarte.
- JaCoCo: FollowupService 70/74 linhas (94,59%); controller e handler 100%. Primeiro run detectou binding sem produto retornando 500 e fixture incompleta; correções validadas na repetição final aprovada.
- CI head aprovado: run 40610606610/job 122560946664. Prévia Railway 599a3767-709f-43a6-90b5-844cb36fe80e conferida no head correto, V64, aplicação iniciada e OpenAPI HTTP 200.
- Produção Railway bd121bb3-2e1b-4051-a4dc-ca3afc10f9b1: merge confirmado, V63→V64 aplicada com sucesso e aplicação iniciada. OpenAPI HTTP 200, follow-ups sem sessão HTTP 403. Nenhuma configuração de produção alterada para corrigir prévia.

## Interface

- PR #61, head 116151cedbcf849163697be26a94777b13e675e2; merge aff2b2bbd6c3319ddbc55c30e52a69916f8e13f1.
- 185 testes/47 arquivos aprovados. 12 casos novos F08. Primeira suíte teve três timeouts em testes antigos sob concorrência; repetição isolada e suíte completa com um worker aprovadas sem alterar timeouts.
- Cobertura dos três arquivos novos board/editor/cliente HTTP: 100% linhas, instruções e funções; 92,26% branches. Tipos e lint sem erros novos; build webpack local aprovado. CI do head final run 40685280202/job 122805923866 aprovada; prévia dpl_Ampz5zrRNULRCJvV7fkswCGLVc5q READY.
- Revisão estática independente de consistência aprovada, sem bloqueadores. Revisão React/manual de segurança aprovada; não há nova dependência de UI. Inspeção visual com dados fictícios em 1440×1000 e 390×844, sem overflow, incluindo edição. Capturas em coordination/evidencias-f08.
- Produção: deploy dpl_5vGkBfekLX1DwrV3NdYJrViczZug READY, SHA aff2b2bbd6c3319ddbc55c30e52a69916f8e13f1 e alias www.agenciashub.com.br confirmados. Site HTTP 200; /viagens/follow-ups e /milhas/follow-ups sem sessão redirecionam para login (307), confirmado também no navegador. API publicada antes da UI; entrega concluída.

## Limites do smoke

Nenhum registro real de negócio foi criado ou modificado em produção para o teste. Persistência/CRUD/concorrência/isolamento foram testados em PostgreSQL efêmero; as capturas de interface usam API simulada e dados fictícios. Smoke de produção confirma commit, migration, inicialização, disponibilidade e proteção sem sessão.

O conector Railway exige reautenticação; verificação realizada pela sessão autenticada do navegador. Não houve alteração de credenciais ou configuração de produção nesta entrega.
