# Diagnóstico público de Milhas — TODO-055 — publicado em 28/10/2026

## Escopo entregue no código
Fila própria de Milhas e formulário público com consentimento versionado, contas/cartões/clubes declarados, contato internacional e revisão autenticada. A agência gerencia link por configuração versionada, pausa e revogação. Cadastro público não escreve em CRM/carteira; incorporação escolhe pessoa e registros, confere versão do perfil e é transacional/idempotente. Histórico preserva original e revisões; CRUD interno inclui edição pelo mesmo ID e arquivamento/reativação por motivo. Comprovante mostra somente a seleção aplicada, sem sugerir alteração dos contatos do CRM. Novo autor da carteira usa ID estável da sessão; históricos antigos não foram reescritos.

## Referências exatas
- Reserva V66: registro central, commit8a3acc7, antes de escrever SQL.
- API PR70 head a6d69839b458ce7ebe64260980fd2d0de85253d8; CI40750808852/job123036654086 SUCCESS.
- UI PR64 head ed199706344d17c506357ee1debdbec2c9e9a9e9; CI40752771478/job123043566632 SUCCESS. Head anterior b0c644c também validado; último ajuste preserva estado arquivado/incorporado em reenvio.
- Prévia UI dpl_9hvMqyGyuTqEht6GWYVnfN8572jh READY no head ed19970.
- Prévia API ambiente cdabbeee-9552-4dab-b51a-00ad5aca81ae, deploy9603271c-6b3e-422b-a789-3b7c6ea0dc70, commit exato confirmado no Railway; ACTIVE no head exato; schema vazio recebeu65migrations atéV66, Started AgenciaHubApplication confirmado às19:10:42UTC. HTTP OpenAPI200, rota protegida403, token público inexistente404 e inválido400.

## Validação concluída
35 testes distintos da API: diagnóstico11, upgrade1, carteira6, diretório3, LGPD8, isolamentoLGPD2, descarte1, rate-limit3. Banco novo e upgradeV65→V66 preservando carteira/versionamento anteriores, sem criar link nem envio automaticamente. Gate de migrations preservou histórico e sequência.

234 testes UI em55arquivos; 18 focados nos quatro componentes novos. Cobertura medida no head b0c644c, antes do último ajuste de reenvio:98,64%linhas/92,04%branches. O head final ed19970 acrescentou o teste de reenvio de registro arquivado/incorporado; suíte completa, tipos/lint e build da CI aprovados. Inspeção desktop/celular com componentes reais e dados fictícios, evidências em docs/coordination/evidencias-diagnostico-milhas. Harness removido; nenhum registro real foi alterado em produção.

Revisão SDD estática independente identificou corrida incorporação×LGPD antes do lock da pessoa. Corrigida com rechecagem após FOR UPDATE e teste concorrente real: incorporação não commitada, guard inicial negativo, liberação do commit, recusa após lock e solicitação PENDING/CRM preservado. Revisão final aprovada, inclusive comprovante parcial. Falha simulada no meio da incorporação prova rollback de perfil, itens, histórico e comprovante. Descarte da agência remove diagnósticos e movimentações antes das pessoas, preservando outra agência.

## Limites explícitos
Não sincroniza programas, consulta CPF ou armazena credenciais/número completo de cartão. Vencimentos continuam no CRUD interno após revisão da conta. Arquivamento preserva declaração e versões; não estorna incorporação. Pedidos LGPD com diagnóstico/histórico continuam pendentes para revisão, inclusive e-mail original/intermediário sem CRM — não há confirmação parcial de anonimização. Nenhum link público é ativado por migration. V66 deve ser publicada e confirmada antes da UI; nenhum ajuste de schema/pool ou variável de produção foi preparado.

## Publicação concluída

API PR70 integrada no merge3a30bafbc2d62bc1d050c6409a7800986ecc7e8e; UI PR64 integrada no mergebe685c874290390d636d6151e4959846407e331b, exclusivamente após API V66/startup/HTTP confirmados. Railway produção f5cb7fad-5e8c-4fde-bb39-2e8494ae485d Active, no commit exato3a30baf. Logs: schema public inicialmente65; migração66 - miles diagnostics aplicada com sucesso às23:15:19UTC; Started AgenciaHubApplication às23:15:23.529UTC. Nenhuma variável, schema de conexão, pool ou configuração alterada.

Vercel dpl_9drBurR6nXYTCCjhNWbt4JyAx2TW READY no merge exato be685c8, target production e alias www.agenciashub.com.br confirmados. HTTP: site200, página pública200, área interna sem sessão307; API OpenAPI200, fila protegida403 sem sessão, token inexistente404/DIAGNOSTIC_ERROR e token inválido400/INVALID_DIAGNOSTIC. Página pública real renderizou o estado de link indisponível para UUID fictício, sem cadastro/envio real. Nenhum link de agência foi ativado para teste; habilitação continua explícita pelo dono em Milhas → Diagnósticos. CRUD e incorporação foram exercitados no banco isolado, não com cadastros reais de produção.

Evidências reais: coordination/evidencias-diagnostico-milhas/api-producao-ativa.png e publico-producao-link-inexistente.png. Capturas anteriores dos formulários completos usam dados fictícios, conforme README.

Histórico do incidente, encerrado para esta entrega: builds a7a9ce63 e89fae589 falharam antes do Maven; diagnóstico Railway Infrastructure Error, primeira falha frontend grpc server closed unexpectedly. A versão anteriorV65 permaneceu ativa durante as falhas. A página oficial https://status.railway.com/incident/Z1JDWW1W passou a Resolved (28/10,19:17UTC), informando builds/deploys normalizados e recomendando refazer os que falharam. Após atualização da página e nova conferência de main, Deploy latest commit gerou f5cb7fad-5e8c-4fde-bb39-2e8494ae485d, sem mudança de código/configuração. Não houve mensagem ao suporte. A captura railway-producao-bloqueada.png é histórica, não representa o estado vigente.

TODO-055 e TODO-043 resolvidas; D1–D4 concluídas. Specs arquivadas em sdd/features/20261028-diagnostico-publico no repositório de coordenação. V66 integrada e aplicada, imutável/não reutilizável. V67 sem reserva; próxima frenteF11/TODO-044. F05 e créditosF09 continuam condicionados às regras de negócio.

Observação de validação: o OpenAPI legado da API responde200 com paths vazios por packages-to-scan antigo; não foi usado como prova do contrato novo. Rotas do diagnóstico foram verificadas diretamente na prévia (403autenticada,404token inexistente e400token inválido), além dos35testes HTTP/SQL locais. Corrigir a configuração geral de documentação é melhoria separada; não alterada nesta entrega.
