# Acompanhamento comercial por produto

A agência precisa saber quem contatar, quando e quem é responsável. A mesma pessoa pode ter acompanhamentos independentes em Viagens e Milhas. Usar pessoas/prospectos do CRM compartilhado; não criar outro funil nem duplicar identidades.

- Tela própria em cada produto, com ação principal Nova ação, filtros de pessoa, responsável, situação e prazo; resumo de ações abertas atrasadas, para hoje e próximas.
- Hoje é a data de Fortaleza informada pela API. Atrasadas têm prazo anterior a hoje; próximas são todas as datas posteriores, sem janela oculta. Prazo é uma data, sem horário.
- Criar exige pessoa, produto, título da próxima ação, prazo e responsável ativo da mesma agência. Observações opcionais. Permitir várias ações; ordenar por prazo e ID para tornar a próxima evidente.
- Consultar lista e histórico; editar título, prazo, responsável e observações pelo mesmo ID. Pessoa e produto são preservados. Reprogramar é uma edição e mantém data anterior no histórico.
- Concluir; cancelar com motivo; reabrir com motivo. Encerradas precisam ser reabertas para editar. Excluir é lógico, somente quando aberta, com confirmação na UI; histórico permanece acessível. Cancelar não exclui o histórico.
- Todos os membros dono/vendedor podem gerenciar registros da própria agência. Admin de plataforma não acessa. Outra agência recebe 404 para registro/histórico.
- Sucesso apenas após API; conflito 409 conserva formulário; falha de rede permite reenvio com a mesma chave e não confirma ausência de gravação. Recarregar mantém dados. Troca de produto/sessão não exibe dados anteriores.
- Histórico tem autor, instante e retratos anterior/novo. Histórico pessoal impede exclusão global da pessoa e conclusão falsa de anonimização; explicar motivo. Exclusão definitiva autorizada da agência remove os registros na ordem correta.
- Não criar viagens, tarefas, compromissos, vendas, lançamentos financeiros nem movimentos de milhas. Não enviar mensagens automaticamente.

Aceite com testes HTTP/PostgreSQL: CRUD, reenvio/concorrência, versões, estados, limites de datas, isolamento, papéis, privacidade e descarte. Testes UI: formulário preservado em erro, confirmação API, edição mesmo ID, exclusão e filtros. Inspeção desktop/celular. Validar banco novo e V63→V64 antes de publicar API e depois UI.
