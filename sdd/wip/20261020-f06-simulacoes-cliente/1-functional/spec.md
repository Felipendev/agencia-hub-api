# TODO-052 — planos de acúmulo por cliente
Calcular continua avulso, sem salvar. Salvar plano exige pessoa da mesma agência, sem criar CRM/perfil/carteira. Planos pertencem a Milhas; não alteram tarefas, saldo, vendas ou viagens. Possuir plano não inclui automaticamente a pessoa no diretório de carteiras; consulta pelo simulador e ficha Milhas.

CRUD preserva IDs, cálculo, versões e exclusão existentes. Corrigir cliente exige confirmação e histórico do vínculo em cada versão. Duplicar é nova criação, com seleção e confirmação do cliente. Legados ficam sem cliente até associação explícita; nunca inferir pelo título/contato nem reescrever versões antigas.

Filtros: todos, pessoa e legados sem vínculo. Ficha Milhas lista os mesmos planos por customerId e abre edição pelo ID; novo plano preseleciona pessoa. Consulta não cria perfil. Carregamento, vazio, falha/recarga; sucesso só após API; conflito mantém formulário. Tenant em todos os IDs/históricos/filtros. Vínculos e versões impedem exclusão/anonimização parcial da pessoa; explicar revisão. Descarte autorizado da agência limpa planos antes de pessoas e preserva outras agências.
