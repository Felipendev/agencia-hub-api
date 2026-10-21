# Registro de migrations — Codex e Claude

Atualizado em 21/10/2026. Autoridade única para reserva: Codex como integrador. Ler antes de escrever SQL; nunca escolher a próxima versão apenas olhando a própria branch. Este registro substitui a instrução anterior de números provisórios.

## Base confirmada

- Produção confirmada em 20/10: API `a9932f8691518ef93dc3ee41328f64d3fe664bd4`, Railway `b603ff5f-acfd-4141-b7f6-cfa052b56bb4` SUCCESS. Schema public em V61, após upgrades sequenciais V58→V59→V60→V61 e inicialização confirmada em cada etapa.
- Interface `a3b67c8a634939a6c77cca093da200b2674734d0`, Vercel `dpl_DBbTbgb8vrswnQ6vms4KJrmhhTm8` READY em www.agenciashub.com.br. Site e OpenAPI HTTP 200; rotas protegidas da API sem sessão HTTP 403.
- V56 a V61 publicadas e imutáveis. Registros: publicacao-agenda-2026-10-20.md, publicacao-checklists-2026-10-20.md e publicacao-simulacoes-cliente-2026-10-20.md.
- A prévia `agencia-hub-api-pr-62` pertence ao incidente histórico de 15/10. O ajuste manual descrito naquele runbook não é mais pré-requisito para a entrega: a API já foi integrada e publicada. Não reaplicar configurações da prévia à produção nem tentar recuperar aquele histórico para encaixar outra numeração.

## Reservas obrigatórias

| Versão | Dono | Feature / branch | Situação |
|---|---|---|---|
| V56 | Claude / integração Codex | F02 contratos | Publicada, imutável |
| V57 | Claude / integração Codex | F02 índices | Publicada, imutável |
| V58 | Claude / integração Codex | F03 economia, `claude/f03-economia-cliente` | Publicada na API, imutável |
| V59 | Claude / integração Codex | F07 agenda/atas, `claude/f07-agenda-atas` | Publicada: API #65 3dca6dc, Railway a45d66e4 SUCCESS/V59; UI #58 615fda0, Vercel READY no domínio principal |
| V60 | Codex | F01 checklists, `codex/f01-checklists` | Publicada: API #63 9f7f224, Railway 1f607de2 SUCCESS/V60; UI #56 04f903b, Vercel READY no domínio principal |
| V61 | Codex | TODO-052 simulações por cliente, `codex/f06-simulacoes-cliente` | Publicada: API #64 a9932f8, Railway b603ff5f SUCCESS/V61; UI #57 a3b67c8, Vercel READY no domínio principal |
| V62 | Claude / integração Codex | TODO-050 extensão F10, `claude/f10-etapas-viagem` | API #66/UI #59 em revisão e integração Codex; uma migration V62 existente, ainda não publicada |
| V63 | Claude | TODO-049 F09 cadastro de indicações, `claude/f09-indicacoes` | Reservada para uma migration inicial do cadastro, sem créditos automáticos; publicar após V62 |

F08, evolução F04/F06, F11 e F05: sem número autorizado por enquanto. Reservar aqui antes de criar migrations. Se precisar de mais de um arquivo, combinar previamente a revisão das reservas; não ocupar número alheio. Não criar migrations vazias para liberar a fila.

## Fluxo de integração

1. Cada agente usa banco de teste próprio e branch/PR isolados. Não apontar testes para produção.
2. Desenvolver em paralelo, publicar em sequência V58 → V59 → V60 → V61 → V62 → V63. Versão maior aguarda as anteriores; sem `outOfOrder` ou ignore de validação.
3. Antes de integrar, buscar main e comparar nomes/conteúdo das migrations publicadas. Não editar/renomear migrations executadas nem usar repair para mascarar divergências.
4. Validar tanto banco novo quanto atualização da base anterior com dados representativos e preservação dos registros. CI de compilação não substitui esse teste.
5. Validar inicialização da aplicação com as configurações reais de schema/pool na prévia; publicar API, confirmar migração e saúde, depois UI. Não mudar configuração de produção para resolver problema da prévia.
6. Atualizar este registro com merge, versão aplicada e evidência. Falha em prévia bloqueia publicação, preservando a última produção saudável.

A CI da API executa `scripts/check-migrations.sh` nos PRs, comparando a árvore candidata com a main atualizada. O gate recusa versões duplicadas, modificação/exclusão de migrations integradas e novos números com lacunas. Validado com uma adição sequencial aceita e três cenários recusados (duplicidade, edição do histórico e V60 sem V59). A proteção foi integrada na main pelo PR #62. Reservas de dono continuam dependendo deste registro e da revisão do integrador.

## Incidente da prévia F03

A orientação anterior adicionou `SPRING_DATASOURCE_HIKARI_CONNECTION_INIT_SQL`. O deploy passou a falhar no binding dessa propriedade porque o pool já estava iniciado. Correção: remover essa variável e selecionar o schema pela URL JDBC, conservando Flyway/Hibernate alinhados ao schema isolado. Não aplicar essas variáveis à produção.

O ajuste exige remover uma variável; o conector disponível não expõe exclusão de variável. A aplicação deve ser feita no painel Railway, no ambiente e serviço exatos indicados no runbook da prévia, após o teste de inicialização. Não apagar Postgres, volume ou dados. Não alterar V56/V57 publicadas.
