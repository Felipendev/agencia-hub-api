# Registro de migrations — Codex e Claude

Atualizado em 28/10/2026. Autoridade única para reserva: Codex como integrador. Ler antes de escrever SQL; nunca escolher a próxima versão apenas olhando a própria branch. Este registro substitui a instrução anterior de números provisórios.

## Base confirmada

- Produção vigente após TODO-053 em 28/10: API ef31e7b6d597b825cb0b062a3ed57dd19bf8592e, Railway 6ad7fb73-f08f-4a74-b632-2491c6495c0b ativo/V65; UI 68ea7124c37e88b6229f0f8a211a7b4408729310, Vercel dpl_9aLrTTQR24HCtAhBGNbm596nqrAW READY em www.agenciashub.com.br. V56–V65 imutáveis. UI atual após TODO-054: b3722b5, Vercel DzZnLAqn READY. V66 reservada abaixo.

- Base anterior F08 em 28/10: API 33f8238ecc6e5d7caed30534d977d505a8aae865, Railway bd121bb3-2e1b-4051-a4dc-ca3afc10f9b1 ativo/V64; UI aff2b2bbd6c3319ddbc55c30e52a69916f8e13f1, Vercel dpl_5vGkBfekLX1DwrV3NdYJrViczZug READY no domínio principal. Registro: publicacao-follow-up-2026-10-28.md.

- Base anterior em 27/10: API `7ef4ce81ce240abfed0bccad77872e90ea2386d6`, Railway `f3abd48b-2cb6-4d34-87e0-73aec7aa5439` ativo/V63; UI `082026434f8d2e31fa0cbbb0ac9c8324872661b6`, Vercel `dpl_GN4Z6tZQRGwjKGvuVg5zRH9dApJH` READY no domínio principal. Registros: publicacao-etapas-2026-10-27.md e publicacao-indicacoes-2026-10-27.md.

- Base histórica publicada em 20/10: API `a9932f8691518ef93dc3ee41328f64d3fe664bd4`, Railway `b603ff5f-acfd-4141-b7f6-cfa052b56bb4` SUCCESS. Schema public em V61, após upgrades sequenciais V58→V59→V60→V61 e inicialização confirmada em cada etapa.
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
| V62 | Claude / integração Codex | TODO-050 extensão F10, `claude/f10-etapas-viagem` | Publicada em 27/10: API #66 882b3f3, Railway 492b242c SUCCESS/V62; UI #59 8f6ade9, Vercel 3La2peo READY no domínio principal. Evidência em publicacao-etapas-2026-10-27.md |
| V63 | Claude / integração Codex | TODO-049 F09 cadastro de indicações, `claude/f09-indicacoes` | Publicada em 27/10 após V62: API #67 7ef4ce8, Railway f3abd48b ativo/V63; UI #60 0820264, Vercel GN4Z6tZ READY. 61 testes API/173 UI repetidos no head integrado. Créditos seguem fora do escopo |
| V64 | Codex | TODO-042 F08 follow-up comercial, `codex/f08-follow-up` | API publicada em 28/10: PR #68 merge 33f8238, Railway bd121bb3 ativo/V64. 70 testes API aprovados, banco novo e upgrade V63→V64. UI #61 merge aff2b2b publicada, Vercel 5vGkBfek READY no domínio principal. 185 testes UI aprovados. Imutável; ver publicacao-follow-up-2026-10-28.md |
| V65 | Codex | TODO-053, subentrega TODO-043: plano → tarefas de Milhas, `codex/f04-diagnostico-integracoes` | Publicada em 28/10: API #69 ef31e7b, Railway 6ad7fb73 ativo/V65; UI #62 68ea712, Vercel 9aLrTTQR READY no domínio principal. 38 testes API/200 UI aprovados; banco novo e V64→V65, startup e domínio confirmados. Imutável. Ver publicacao-plano-tarefas-2026-10-28.md. |

| V66 | Codex | TODO-055 diagnóstico público de Milhas, `codex/f04-diagnostico-publico` | Reservada em 28/10 antes do SQL; configuração de link, declarações revisáveis, versões e comprovante de incorporação. Base V65, banco novo e upgrade V65→V66 obrigatórios. |

Cartões TODO-054 não exigiram migration. F11 e F05 seguem sem número autorizado. V56–V65 publicadas e imutáveis. Não ocupar número alheio nem criar migrations vazias para liberar a fila.

## Fluxo de integração

1. Cada agente usa banco de teste próprio e branch/PR isolados. Não apontar testes para produção.
2. Desenvolver em paralelo, publicar em sequência V58 → V59 → V60 → V61 → V62 → V63 → V64 → V65 → V66. Versão maior aguarda as anteriores; sem `outOfOrder` ou ignore de validação.
3. Antes de integrar, buscar main e comparar nomes/conteúdo das migrations publicadas. Não editar/renomear migrations executadas nem usar repair para mascarar divergências.
4. Validar tanto banco novo quanto atualização da base anterior com dados representativos e preservação dos registros. CI de compilação não substitui esse teste.
5. Validar inicialização da aplicação com as configurações reais de schema/pool na prévia; publicar API, confirmar migração e saúde, depois UI. Não mudar configuração de produção para resolver problema da prévia.
6. Atualizar este registro com merge, versão aplicada e evidência. Falha em prévia bloqueia publicação, preservando a última produção saudável.

A CI da API executa `scripts/check-migrations.sh` nos PRs, comparando a árvore candidata com a main atualizada. O gate recusa versões duplicadas, modificação/exclusão de migrations integradas e novos números com lacunas. Validado com uma adição sequencial aceita e três cenários recusados (duplicidade, edição do histórico e V60 sem V59). A proteção foi integrada na main pelo PR #62. Reservas de dono continuam dependendo deste registro e da revisão do integrador.

## Incidente da prévia F03

A orientação anterior adicionou `SPRING_DATASOURCE_HIKARI_CONNECTION_INIT_SQL`. O deploy passou a falhar no binding dessa propriedade porque o pool já estava iniciado. Correção: remover essa variável e selecionar o schema pela URL JDBC, conservando Flyway/Hibernate alinhados ao schema isolado. Não aplicar essas variáveis à produção.

O ajuste exige remover uma variável; o conector disponível não expõe exclusão de variável. A aplicação deve ser feita no painel Railway, no ambiente e serviço exatos indicados no runbook da prévia, após o teste de inicialização. Não apagar Postgres, volume ou dados. Não alterar V56/V57 publicadas.
