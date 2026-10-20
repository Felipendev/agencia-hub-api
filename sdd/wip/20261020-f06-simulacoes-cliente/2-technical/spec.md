# Contrato
V61: customer_id nullable nas simulações e versões, FKs/índices, sem backfill inferido. POST {customerId,assumptions} exige cliente; PATCH {customerId,assumptions,expectedVersion}. PATCH sem customerId mantém vínculo existente; legado sem vínculo exige associação. Conferir versão antes de no-op; stale 409. DELETE aceita expectedVersion opcional por compatibilidade, UI sempre envia.

GET aceita customerId ou unlinked=true. Saved/Version incluem vínculo; autor novo UUID da sessão, nome só resolvido na mesma agência. Não inventar autores legados. Cálculo permanece puro.

UI /milhas/simulador com ClientePicker, filtros separados, simulationId/customerId de URL em Suspense; componente de planos no perfil. Branch independente codex/f06-simulacoes-cliente a partir de main/V58, V61 reservada. Não publicar antes de V59/V60; upgrade local V58→V61 não substitui V60→V61. Gate/PRs em rascunho preservados. POST novo intencionalmente recusa payload antigo sem cliente; API antes de UI e refresh da interface ao publicar.
