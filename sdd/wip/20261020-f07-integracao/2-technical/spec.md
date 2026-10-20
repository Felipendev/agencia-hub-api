# Integração e revisão

V59 de Claude preservada sem edição/renumeração. PRs API #65 e UI #58, branches claude/f07-agenda-atas em worktrees próprios. Handler de FK para pessoa/responsável; bloqueio antes de excluir pessoa inclui appointment_events e lock da pessoa para serializar com criação. Anonimização segue padrão contratos/economia, sem confirmação parcial. Scheduler exclui agenda antes de viagens/pessoas/usuários.

Revisão corrige idempotência comparando todos os campos normalizados e versão de status verificada antes de no-op; testes concorrentes. UI: menu Principal dono/vendedor, matcher /agenda e subrotas, atalhos TRAVEL/MILES da pessoa, remount da lista por querystring, recusa de horário inexistente no fuso ao converter. Publicar API só após testes/CI/upgrade/saúde de prévia, depois UI. V60 e V61 aguardam integração e testes de seus upgrades reais.
