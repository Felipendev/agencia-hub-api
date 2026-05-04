-- Adiciona categoria MILES ao enum (PostgreSQL usa VARCHAR, sem ALTER TYPE necessário)
-- A coluna já é VARCHAR(32), basta garantir que o valor seja aceito pela aplicação.

-- Adiciona campo de conta bancária (opcional) para organizar lançamentos por conta
ALTER TABLE financial_entries
    ADD COLUMN bank_account VARCHAR(128);

CREATE INDEX idx_financial_entries_bank_account ON financial_entries (bank_account)
    WHERE bank_account IS NOT NULL;
