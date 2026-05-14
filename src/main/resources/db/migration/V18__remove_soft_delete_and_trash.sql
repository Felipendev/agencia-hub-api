-- Sem lixeira: exclusão de cotação é permanente; remove colunas e índices de soft delete.

DROP INDEX IF EXISTS idx_quotations_deleted;
DROP INDEX IF EXISTS idx_quotations_active;
ALTER TABLE quotations DROP COLUMN IF EXISTS deleted_at;
CREATE INDEX IF NOT EXISTS idx_quotations_created_at ON quotations (created_at DESC);

DROP INDEX IF EXISTS idx_customers_deleted;
DROP INDEX IF EXISTS idx_customers_active;
ALTER TABLE customers DROP COLUMN IF EXISTS deleted_at;
CREATE INDEX IF NOT EXISTS idx_customers_created_at ON customers (created_at DESC);
