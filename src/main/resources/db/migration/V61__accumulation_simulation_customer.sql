-- TODO-052/Codex: publicar somente após V59/F07 e V60/F01. Não inferir clientes de cenários legados.
ALTER TABLE accumulation_simulations ADD COLUMN customer_id UUID REFERENCES customers(id);
ALTER TABLE accumulation_simulation_versions ADD COLUMN customer_id UUID REFERENCES customers(id);
CREATE INDEX accumulation_simulations_customer_idx ON accumulation_simulations(agency_id,customer_id,updated_at DESC);
CREATE INDEX accumulation_simulation_versions_customer_idx ON accumulation_simulation_versions(customer_id,simulation_id);
-- NULL conserva ausência de vínculo de cada versão legada. Associação explícita gera nova versão.
