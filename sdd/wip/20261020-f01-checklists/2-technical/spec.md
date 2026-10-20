# Implementação

Spring/JDBC transacional em módulo tasks, DTOs validados, tenant e usuário resolvidos da sessão. Tabelas V60: checklist_templates, checklist_runs, work_tasks e work_task_events; JSONB para etapas/snapshot e eventos, versão otimista com lock de registro. Lock no modelo serializa aplicação; unique por agência/modelo/pessoa/chave do período. Tarefas avulsas usam Idempotency-Key por agência. Histórico de tarefa, execução e modelo guarda retratos e UUID do autor.

API /work: /tasks CRUD + /state + /history; /templates CRUD + /apply; /runs consulta/edição/cancelamento; /members lista somente id/nome de membros ativos para OWNER/SELLER. Recurso não pertencente à agência retorna 404. Conflitos de versão/estado retornam 409; payload inválido 400.

Next: /tarefas?product=TRAVEL ou MILES, abas Pendências/Modelos/Execuções, formulários com ClientePicker, estados de carregamento/erro/vazio e ações de domínio. Entradas nos menus de cada produto. Usar componentes e tokens existentes, formulários responsivos e sem dependência de novo sistema visual.

Publicação bloqueada pela reserva V59/Claude: V60 não será renumerada, nem aplicada em ambiente compartilhado antes da V59. Testes locais usam PostgreSQL efêmero exclusivo. Gate de sequência de main deve recusar V60 até V59 integrada; não desativar o gate. PRs em rascunho isolados e pareados. Após V59, atualizar branch e repetir migrations/upgrade antes de merge API e UI.
