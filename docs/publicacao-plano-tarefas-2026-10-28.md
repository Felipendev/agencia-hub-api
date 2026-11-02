# TODO-053 — Plano de Milhas → tarefas

Subentrega da TODO-043 em 28/10/2026. Não encerra a TODO-043 inteira: diagnóstico público e reaproveitamento de cartões seguem pendentes, em entregas separadas. Produto Milhas, identidade CRM compartilhada.

## Comportamento

Plano salvo vinculado à pessoa permite revisar 1–20 tarefas com título, prazo, responsável ativo e observações. Geração explícita, atômica, uma vez por plano; confirmação simultânea/reenvio não duplica. Após gerar, CRUD/estados/histórico usam o F01 e o mesmo ID. Pessoa/produto das tarefas de origem são preservados por API e banco. Não altera carteira, saldos, viagens, vendas ou financeiro.

Comprovante imutável registra autor/data, pessoa, versão e premissas do plano e tarefas revisadas originalmente; também consulta os dados atuais das tarefas. Alterar o plano não altera tarefas. Excluir plano e todas as tarefas preserva consulta no Histórico de gerações, por source_id; não libera nova geração. A revisão identificou inicialmente o comprovante sem acesso após excluir plano; o bloqueador foi corrigido e reaprovado, com testes. O comprovante é auditoria, não um cadastro editável; edição/exclusão aplicável é realizada nas tarefas F01. Pedidos de privacidade com histórico continuam pendentes para revisão. Descarte definitivo de agência remove os vínculos/comprovantes na ordem correta.

## API — PR #69

- Head c3692531f7d2a07bc23619ecd870d16f84bf6d20; merge ef31e7b6d597b825cb0b062a3ed57dd19bf8592e. Base main 33f8238/V64. V65 reservada e publicada no registro central antes do SQL; duas tabelas de geração/vínculos, sem backfill e sem alterar migrations publicadas.
- 38 testes JUnit reais aprovados no head final, zero falhas/erros/skips, Spring/HTTP/PostgreSQL efêmero; 9 casos novos. Cobrem CRUD/estados, payload/versão, concorrência, rollback integral quando a segunda tarefa falha, origem, pessoa/produto, exclusões, tenant/papéis, LGPD/descarte. Regressão anterior ampliada de 55 casos aprovada antes do ajuste de consulta histórica.
- Banco novo e upgrade V64→V65 com pessoa, plano/premissas e tarefa anteriores preservados. Um UUID de fixture/asserção foi corrigido ao reforçar a amostra; repetição aprovada. Gate de migrations aprovado localmente via Git Bash e pela CI; WSL não tem bash e não foi usado para contornar o gate.
- JaCoCo: SimulationTaskService 38/41 linhas (92,68%); controller e handler 100%. CI run 40729695749/job 122962349902 aprovada no head correto.
- Prévia Railway agencia-hub-api-pr-69, ambiente 73da5625-458d-452e-9623-5d9da476a2a5, deploy 888e8671-e4f7-48ae-8a00-84754c2fbb53: commit conferido, banco novo até V65 e aplicação iniciada; OpenAPI 200 e acesso sem sessão 403, antes do merge.

## Interface — PR #62

- Head a4172baf59a3eb37ac761882b5e50126e7ccbf29; merge 68ea7124c37e88b6229f0f8a211a7b4408729310. Rotas /milhas/simulador/[id]/tarefas e /milhas/simulador/tarefas-geradas, links no simulador e origem/filtro no F01. Pessoa de tarefa gerada não pode mudar; demais campos editáveis no mesmo registro.
- 200 testes/50 arquivos aprovados no head final. 23 testes focados com cobertura dos três arquivos novos de UI/cliente HTTP: 100% linhas/instruções/funções, 95,37% branches. Tipos/lint sem erros novos, build webpack aprovado e CI run 40729701323/job 122962369106 aprovada.
- Prévia Vercel inicial dpl_GAoTUG8xzFDq71PXWfmAhDJUL5ke falhou no resolvedor de fontes Turbopack (next/font/google queries have exactly one entry). Repetição do mesmo commit sem alterar fonte/configuração: dpl_2Z1vX3mSjMxEEgK6wB8HiRPSinJq READY. Nenhuma versão com build falho foi publicada.
- Revisão estática independente e React/manual de segurança aprovadas após correção da consulta histórica. Inspeção em 1440×1000/390×844, sem overflow, incluindo foco no comprovante e acesso a origem excluída. Evidências com API simulada/dados fictícios em coordination/evidencias-plano-tarefas.

## Publicação e limites

API publicada antes da UI. Railway 6ad7fb73-f08f-4a74-b632-2491c6495c0b ativo no merge ef31e7b: V64→V65 aplicada, aplicação iniciada, OpenAPI 200 e diretório sem sessão 403. UI no merge 68ea712: Vercel dpl_9aLrTTQR24HCtAhBGNbm596nqrAW READY e alias www.agenciashub.com.br confirmados. Site 200 e novas rotas sem sessão redirecionam ao login (307). TODO-053 concluída e publicada. Evidência: coordination/evidencias-plano-tarefas/api-producao.png.

Nenhum cadastro real foi modificado para o smoke. CRUD/isolamento/concorrência são testados em PostgreSQL efêmero; capturas de produto usam dados fictícios. Smoke de produção confirma versão, migration, inicialização, disponibilidade e acesso sem sessão. Conector Railway exige reautenticação; validação usa a sessão do navegador. Não alterar configuração de produção para reparar prévia.
