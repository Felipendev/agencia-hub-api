-- Soft delete: adiciona campo deleted_at nas tabelas quotations e customers

ALTER TABLE quotations ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE customers ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

-- Índices parciais para queries de listagem (registros ativos)
CREATE INDEX idx_quotations_active ON quotations (created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_customers_active ON customers (created_at DESC) WHERE deleted_at IS NULL;

-- Índices parciais para queries da lixeira (registros excluídos)
CREATE INDEX idx_quotations_deleted ON quotations (deleted_at DESC) WHERE deleted_at IS NOT NULL;
CREATE INDEX idx_customers_deleted ON customers (deleted_at DESC) WHERE deleted_at IS NOT NULL;
