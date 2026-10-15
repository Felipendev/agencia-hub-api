-- Sustenta a contagem de contratos por modelo sem varrer todos os contratos da agência.
CREATE INDEX contracts_template_idx ON contracts(agency_id, template_id);
