# Validação local aprovada — publicação pendente

API: 27 testes selecionados aprovados, PostgreSQL efêmero. UI: 123 testes em 34 arquivos, tipos/build aprovados, lint zero erros e dois avisos preexistentes de cotações. Revisão manual aprovada com gate de release ainda fechado. Desktop 1440×1000 e celular 390×844 inspecionados com dados fictícios e API interceptada no navegador; nenhuma chamada de negócio real. Evidências em docs/evidencias-simulacoes-cliente.

CRUD com cliente, versões/autor, associação legada, isolamento/papéis, concorrência, privacidade e descarte por agência cobertos. Não há criação de carteira, alteração de saldo ou financeiro/Viagens.

O upgrade local V58→V61 não substitui V60→V61. Antes de publicar: integrar V59/V60, preservar ambos os blocos de privacidade/descarte/conflitos e atalhos do perfil, revalidar a sequência e ambiente próprio; API antes da UI. Originais com login/stash e branches F01 preservados.
