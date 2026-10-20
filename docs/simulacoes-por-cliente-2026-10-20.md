# TODO-052 — Simulações vinculadas ao cliente de Milhas

Implementação independente em `codex/f06-simulacoes-cliente`, baseada em main/V58. **Não publicada**: V61 reservada, aguarda V59/Claude e V60/F01 antes de integrar. Não renumerar, criar placeholders ou desativar o gate de sequência.

## Comportamento

O cálculo continua avulso e não grava dados. Salvar plano exige escolher pessoa do CRM da mesma agência. Perfil e simulador consultam os mesmos registros pelo customerId; abrir a ficha de Milhas não cria plano, conta ou carteira. Possuir um plano não inclui automaticamente a pessoa no diretório de carteiras: localizar pelo filtro do simulador e abrir a ficha compartilhada da pessoa. Produtos Viagens e Milhas permanecem separados.

Criar, consultar, editar e excluir continuam disponíveis. Edição mantém o ID e usa expectedVersion. Trocar cliente ou associar legado exige confirmação; grava nova versão com o cliente atual, preservando o vínculo de todas as versões anteriores. Duplicar prepara nova criação com cliente explícito e confirmação; duplicar versão legada não infere pessoa atual. Exclusão remove plano e versões, conforme o CRUD anterior, com aviso e versão consultada na interface.

Filtro de planos da agência por pessoa ou “Somente cenários antigos sem cliente”; filtro separado do cliente do formulário. Alterar um plano para outra pessoa o remove da lista filtrada anterior. Carregamento e falha não aparecem como uma lista vazia bem-sucedida. Erro/409 mantém formulário. Abertura por URL inválida não habilita criação silenciosa de outro registro; limpar formulário é ação explícita.

## Contrato da API

- GET /accumulation-simulations: customerId opcional, validado na agência; unlinked=true apenas para legados sem vínculo. Combinar ambos é 400.
- POST /accumulation-simulations: `{customerId, assumptions}`, cliente obrigatório. Payload antigo sem cliente é recusado deliberadamente; não há fallback para criar plano órfão. Publicar API antes da UI, atualizar a interface e orientar recarga dos clientes antigos no lançamento.
- PATCH /accumulation-simulations/{id}: `{customerId, assumptions, expectedVersion}`. Campo customerId ausente em cliente antigo mantém vínculo existente; cenário legado ainda exige associação. Verificar versão antes de no-op: reenvio desatualizado é 409 e não cria versão adicional.
- DELETE /accumulation-simulations/{id}: expectedVersion opcional por compatibilidade; nova interface sempre envia a versão para recusar exclusão de um plano que mudou.
- Saved/Version incluem customerId/customerName; autor novo vem do UUID real da sessão. authorName é resolvido somente na mesma agência; autores legados não identificados são apresentados como tal, sem reescrever seus registros.

## Dados, privacidade e integração

V61 adiciona customer_id nullable às tabelas atuais e de versões, com FKs e índices. Nenhum cliente é inferido por nome, contato ou título. Versões legadas permanecem sem vínculo após associação explícita da versão atual.

Histórico referenciando uma pessoa bloqueia exclusão silenciosa e processamento parcial de anonimização, inclusive após mover o plano para outro cliente. A solicitação permanece pendente de revisão dos textos/versões, com mensagem. Não foi criada política de retenção automática. Descarte autorizado da agência apaga seus planos/versões antes das pessoas e preserva outras agências.

Somente gestor/vendedor da agência; IDs estrangeiros recusados em criação, consulta, filtros, edição, histórico e exclusão. Nenhuma alteração em saldo, perfil/carteira, financeiro, vendas, tarefas ou viagens. Ao integrar após F01, preservar ambos os blocos de verificação de privacidade, tratamento de conflitos e descarte da agência; não sobrescrever o módulo de tarefas.

## Validação e publicação pendente

Testes HTTP usam PostgreSQL efêmero próprio. Incluem vínculo e troca pelo mesmo ID, versões, concorrência, legado, autor, isolamento e papéis, exclusão/anonimização, descarte e ausência de efeitos financeiros/carteira. Migração local V58→V61 conserva ID, premissas, autor e ausência de vínculos; **não substitui** upgrade real V60→V61 quando os módulos anteriores forem integrados.

Frontend: seleção/obrigatoriedade, edição, duplicação, conflitos, confirmação de associação, filtro, abertura por URL e planos no perfil. Antes de publicar: atualizar com V59/V60, resolver os arquivos compartilhados, revalidar migrations e regressões, confirmar CI/ambiente próprio, publicar API e depois UI. Manter PRs em rascunho até cumprir esses passos.

Resultados em 20/10: **27 testes backend e 123 frontend aprovados**. `mvn test -Dtest=SimulationCustomerIntegrationTest,SimulationCustomerMigrationTest,AccumulationSimulationCrudIntegrationTest,LibraryAndAccumulationIntegrationTest,DataDeletionIntegrationTest,DataDeletionMultiTenancyIntegrationTest,AgencyDeletionSchedulerIntegrationTest`; UI `npm test`, `npm run lint`, `npm run build` e checagem de tipos. Lint sem erros, duas diretivas antigas em cotações fora do escopo. Revisão manual registrada em sdd/wip/20261020-f06-simulacoes-cliente/verdicts/code_review.json; cobertura percentual não medida.

Evidências visuais locais: [desktop](evidencias-simulacoes-cliente/desktop.png), [edição no celular](evidencias-simulacoes-cliente/mobile-edicao.png), [perfil](evidencias-simulacoes-cliente/perfil.png). Dados fictícios, requisições de negócio interceptadas, sem alterações na API real. Layout verificado em 1440×1000 e 390×844, sem rolagem horizontal global. Esses testes não são validação autenticada em produção.
