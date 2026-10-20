# F01 — Checklists, tarefas e rotinas

Gestor e vendedor da mesma agência criam, consultam, editam e excluem tarefas abertas e modelos. Toda tarefa exige pessoa e produto explícito TRAVEL/MILES; responsável é membro ativo da mesma agência. Cotação/viagem são opcionais, exclusivos de TRAVEL e devem pertencer à pessoa. Nenhum efeito em carteira, financeiro ou vendas.

Modelos têm nome, produto, frequência ONCE/WEEKLY/MONTHLY e até 50 etapas com prazo relativo em dias. Aplicar exige pessoa, data-base e responsável opcional. Semana é segunda a domingo; mês é calendário; ONCE tem ocorrência única por modelo/pessoa. Repetir aplicação retorna a execução existente, sem duplicação, inclusive após editar o modelo. Não há agendamento automático nesta entrega.

Execuções preservam uma cópia do modelo e período. Podem consultar/editar título e observações pelo mesmo ID; cancelar preserva tarefas concluídas e cancela as abertas. Não excluir execuções nem reescrever sua pessoa/produto/período para preservar a rastreabilidade; informar essa regra na interface. Remover modelo impede futuras aplicações, preservando execuções.

Tarefas abertas aceitam edição pelo mesmo ID e exclusão lógica auditada. Concluir/cancelar registra autor/data; reabrir é explícito. Cancelar exige motivo. Concluídas/canceladas precisam ser reabertas antes de editar/excluir; tarefa de execução cancelada não pode reabrir. Histórico inclui valores anteriores por evento, sem sobrescrever versões. Filtros por pessoa, responsável, produto, situação e vencimento. Prazo é data civil, sem conversão implícita de fuso.

Aceite: persistência após recarregar, sucesso só após API, falhas preservam formulário, controle de versão em toda mutação, isolamento de agência em todos os IDs e auditoria, repetição/concorrência não duplicam gerações, exclusão de modelo não destrói execução, dados de outros produtos preservados, CRUD e cancelamentos acessíveis em desktop/celular. Anonimização com histórico pessoal exige revisão explícita; descarte autorizado da agência remove o módulo sem afetar outra agência.
