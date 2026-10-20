# Registro de migrations — Codex e Claude

Atualizado em 20/10/2026. Autoridade única para reserva: Codex como integrador. Ler antes de escrever SQL; nunca escolher a próxima versão apenas olhando a própria branch. Este registro substitui a instrução anterior de números provisórios.

## Base confirmada

- Produção confirmada em 20/10: API `cc5079b97bd6bbf9c70d2bf53f4ea3e665b891ff`, PR #62 integrado, deploy Railway `70ad7fb6-a722-46e5-99c8-50c226379a7b` SUCCESS. Logs confirmam atualização do schema public de V57 para V58 e inicialização concluída.
- V58 `trip_savings` já publicada na API. V56/V57 de contratos preservadas. Site e OpenAPI retornaram HTTP 200; rota /savings sem autenticação retorna 403.
- A prévia `agencia-hub-api-pr-62` pertence ao incidente histórico de 15/10. O ajuste manual descrito naquele runbook não é mais pré-requisito para a entrega: a API já foi integrada e publicada. Não reaplicar configurações da prévia à produção nem tentar recuperar aquele histórico para encaixar outra numeração.

## Reservas obrigatórias

| Versão | Dono | Feature / branch | Situação |
|---|---|---|---|
| V56 | Claude / integração Codex | F02 contratos | Publicada, imutável |
| V57 | Claude / integração Codex | F02 índices | Publicada, imutável |
| V58 | Claude / integração Codex | F03 economia, `claude/f03-economia-cliente` | Publicada na API, imutável |
| V59 | Claude / integração Codex | F07 agenda/atas, `claude/f07-agenda-atas` | API #65/UI #58 em revisão; 26 testes API/127 UI, aguarda CI/prévia/publicação |
| V60 | Codex | F01 checklists, `codex/f01-checklists` | Implementada, API #63/UI #56 em rascunho; aguarda V59 e validação do upgrade, não publicada |
| V61 | Codex | TODO-052 simulações por cliente, `codex/f06-simulacoes-cliente` | Implementada, API #64/UI #57 em rascunho; aguarda V59/V60 e upgrade sequencial, não publicada |
| V62 | Claude | TODO-050 extensão F10, `claude/f10-etapas-viagem` | Reservada, uma migration inicial; publicar após V61 |

F08, F09, evolução F04/F06, F11 e F05: sem número autorizado por enquanto. Reservar aqui antes de criar migrations. Se precisar de mais de um arquivo, combinar previamente a revisão das reservas; não ocupar número alheio. Não criar migrations vazias para liberar a fila.

## Fluxo de integração

1. Cada agente usa banco de teste próprio e branch/PR isolados. Não apontar testes para produção.
2. Desenvolver em paralelo, publicar em sequência V58 → V59 → V60 → V61 → V62. Versão maior aguarda as anteriores; sem `outOfOrder` ou ignore de validação.
3. Antes de integrar, buscar main e comparar nomes/conteúdo das migrations publicadas. Não editar/renomear migrations executadas nem usar repair para mascarar divergências.
4. Validar tanto banco novo quanto atualização da base anterior com dados representativos e preservação dos registros. CI de compilação não substitui esse teste.
5. Validar inicialização da aplicação com as configurações reais de schema/pool na prévia; publicar API, confirmar migração e saúde, depois UI. Não mudar configuração de produção para resolver problema da prévia.
6. Atualizar este registro com merge, versão aplicada e evidência. Falha em prévia bloqueia publicação, preservando a última produção saudável.

A CI da API executa `scripts/check-migrations.sh` nos PRs, comparando a árvore candidata com a main atualizada. O gate recusa versões duplicadas, modificação/exclusão de migrations integradas e novos números com lacunas. Validado com uma adição sequencial aceita e três cenários recusados (duplicidade, edição do histórico e V60 sem V59). A proteção foi integrada na main pelo PR #62. Reservas de dono continuam dependendo deste registro e da revisão do integrador.

## Incidente da prévia F03

A orientação anterior adicionou `SPRING_DATASOURCE_HIKARI_CONNECTION_INIT_SQL`. O deploy passou a falhar no binding dessa propriedade porque o pool já estava iniciado. Correção: remover essa variável e selecionar o schema pela URL JDBC, conservando Flyway/Hibernate alinhados ao schema isolado. Não aplicar essas variáveis à produção.

O ajuste exige remover uma variável; o conector disponível não expõe exclusão de variável. A aplicação deve ser feita no painel Railway, no ambiente e serviço exatos indicados no runbook da prévia, após o teste de inicialização. Não apagar Postgres, volume ou dados. Não alterar V56/V57 publicadas.
