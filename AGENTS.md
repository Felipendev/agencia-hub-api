# Requisitos de produto

## CRUD completo

- Toda funcionalidade que gerencia registros editáveis deve entregar criação, listagem/consulta, edição e exclusão, com API persistida e ações disponíveis na interface do AgenciaHub.
- Duplicar registros ou salvar alterações como novos cenários não substitui a edição do registro existente.
- Validar permissões, isolamento entre agências, persistência após recarregar e falhas sem confirmação falsa de sucesso.
- Quando alteração/exclusão direta não fizer sentido por integridade financeira ou histórico, implementar cancelar, arquivar, estornar ou versionar, conforme o domínio, e documentar o motivo.
- Não marcar a feature como concluída enquanto faltar uma operação aplicável do CRUD.
- Preservar alterações locais não relacionadas, incluindo o login exclusivo do ambiente de desenvolvimento.
