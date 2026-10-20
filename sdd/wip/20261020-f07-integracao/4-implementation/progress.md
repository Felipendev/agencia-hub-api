# Revisão local

127 testes frontend aprovados, build/tipos aprovados; lint sem erros e dois avisos preexistentes de cotações. Visual da integração em 1440×1000 e 390×844 com dados fictícios/API interceptada, sem overflow global. Atalho do perfil de Milhas abriu a agenda com customerId e product=MILES.

API: suíte final com 29 testes aprovada, incluindo upgrade V58→V59, configuração de repair desativada por padrão e inicialização real em schema isolado. A falha inicial da fixture sem notes foi corrigida. A prévia Railway falhou por UnknownHost do Postgres, que não possui implantação nesse ambiente. Alteração isolada de Sleep Application=false preparada; accept-deploy recusado pelo conector com “the user did not approve this action”. Produção não alterada e merge/publicação bloqueados até inicialização saudável da prévia.

Revisão corrigiu idempotência com comparação completa dos campos, versão antes de no-op em status e horário inexistente em fuso. Navegação/proteção/atalhos por produto concluídos. Exclusão/anonimização considera eventos de compromissos excluídos; scheduler limpa os quatro conjuntos da agenda antes de pessoas/usuários/viagens.
