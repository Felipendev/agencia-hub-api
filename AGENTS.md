# Requisitos de produto

## CRUD completo

- Toda funcionalidade que gerencia registros editáveis deve entregar criação, listagem/consulta, edição e exclusão, com API persistida e ações disponíveis na interface do AgenciaHub.
- Duplicar registros ou salvar alterações como novos cenários não substitui a edição do registro existente.
- Validar permissões, isolamento entre agências, persistência após recarregar e falhas sem confirmação falsa de sucesso.
- Quando alteração/exclusão direta não fizer sentido por integridade financeira ou histórico, implementar cancelar, arquivar, estornar ou versionar, conforme o domínio, e documentar o motivo.
- Não marcar a feature como concluída enquanto faltar uma operação aplicável do CRUD.
- Preservar alterações locais não relacionadas, incluindo o login exclusivo do ambiente de desenvolvimento.

## Coordenação de migrations

- Antes de criar SQL, ler `docs/migrations-reservadas-2026-10-15.md`. Não usar numeração provisória nem ocupar reserva de outro agente.
- Base publicada V57; F03 V58; Claude F07 V59; Codex F01 V60; Codex simulações por cliente V61. Integrar nessa ordem, com atualização de main e teste de upgrade; outras features exigem reserva prévia do integrador.
- Não editar/renomear migrations já executadas, usar repair para esconder divergências, ignorar validação ou habilitar outOfOrder. Banco novo e atualização precisam ser testados antes de publicar.
- Configurações de schema da prévia F03 não podem ser copiadas para produção. Ver `docs/railway-previa-f03-recuperacao-2026-10-15.md`.
