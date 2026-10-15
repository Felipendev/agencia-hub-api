-- Produtos explícitos; registros de previews anteriores não recebem classificação presumida.
ALTER TABLE contract_templates ADD COLUMN product VARCHAR(16) CHECK (product IN ('TRAVEL', 'MILES'));
ALTER TABLE contracts ADD COLUMN product VARCHAR(16) CHECK (product IN ('TRAVEL', 'MILES'));
CREATE INDEX contracts_product_idx ON contracts(agency_id, product, customer_id);
CREATE INDEX contracts_template_idx ON contracts(agency_id, template_id);
