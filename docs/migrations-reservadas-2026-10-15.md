# Registro de migrations — Codex e Claude

Atualizado em 15/10/2026. Autoridade única para reserva: Codex como integrador. Ler antes de escrever SQL; nunca escolher a próxima versão apenas olhando a própria branch. Este registro substitui a instrução anterior de números provisórios.

## Base confirmada

- Produção: main da API `41a50d9ac3ca236c76e10163fed9ddb2bfec6ed3`, V56 contratos e V57 índices publicados. Deploy `6b103d49-5c23-4595-a511-7ed85e9af192` SUCCESS; site e OpenAPI retornaram HTTP 200 na verificação deste incidente.
- F03: V58 `trip_savings`, PR API #62, ainda não publicado. Não dizer que produção já tem V58.
- Prévia `agencia-hub-api-pr-62`: histórico experimental antigo preservado em public. Esquema novo de revisão: `f03_v58_review`; não reutilizar nem reparar o histórico antigo para encaixar outra numeração.

## Reservas obrigatórias

| Versão | Dono | Feature / branch | Situação |
|---|---|---|---|
| V56 | Claude / integração Codex | F02 contratos | Publicada, imutável |
| V57 | Claude / integração Codex | F02 índices | Publicada, imutável |
| V58 | Claude / integração Codex | F03 economia, `claude/f03-economia-cliente` | Implementada, publicação pendente |
| V59 | Claude | F07 agenda/atas, `claude/f07-agenda-atas` | Reservada, uma migration inicial |
| V60 | Codex | F01 checklists, `codex/f01-checklists` | Reservada, uma migration inicial |
| V61 | Codex | TODO-052 simulações por cliente, `codex/f06-simulacoes-cliente` | Reservada, uma migration inicial |

F08, F09, extensão F10, evolução F04/F06, F11 e F05: sem número autorizado por enquanto. Reservar aqui antes de criar migrations. Se precisar de mais de um arquivo, combinar previamente a revisão das reservas; não ocupar número alheio. Não criar migrations vazias para liberar a fila.

## Fluxo de integração

1. Cada agente usa banco de teste próprio e branch/PR isolados. Não apontar testes para produção.
2. Desenvolver em paralelo, publicar em sequência V58 → V59 → V60 → V61. Versão maior aguarda as anteriores; sem `outOfOrder` ou ignore de validação.
3. Antes de integrar, buscar main e comparar nomes/conteúdo das migrations publicadas. Não editar/renomear migrations executadas nem usar repair para mascarar divergências.
4. Validar tanto banco novo quanto atualização da base anterior com dados representativos e preservação dos registros. CI de compilação não substitui esse teste.
5. Validar inicialização da aplicação com as configurações reais de schema/pool na prévia; publicar API, confirmar migração e saúde, depois UI. Não mudar configuração de produção para resolver problema da prévia.
6. Atualizar este registro com merge, versão aplicada e evidência. Falha em prévia bloqueia publicação, preservando a última produção saudável.

A CI da API passa a executar `scripts/check-migrations.sh` nos PRs, comparando a árvore candidata com a main atualizada. O gate recusa versões duplicadas, modificação/exclusão de migrations integradas e novos números com lacunas. Validado com uma adição sequencial aceita e três cenários recusados (duplicidade, edição do histórico e V60 sem V59). A proteção passa a valer na main após integrar o PR #62; antes disso, não alegar proteção já publicada. Reservas de dono continuam dependendo deste registro e da revisão do integrador.

## Incidente da prévia F03

A orientação anterior adicionou `SPRING_DATASOURCE_HIKARI_CONNECTION_INIT_SQL`. O deploy passou a falhar no binding dessa propriedade porque o pool já estava iniciado. Correção: remover essa variável e selecionar o schema pela URL JDBC, conservando Flyway/Hibernate alinhados ao schema isolado. Não aplicar essas variáveis à produção.

O ajuste exige remover uma variável; o conector disponível não expõe exclusão de variável. A aplicação deve ser feita no painel Railway, no ambiente e serviço exatos indicados no runbook da prévia, após o teste de inicialização. Não apagar Postgres, volume ou dados. Não alterar V56/V57 publicadas.
