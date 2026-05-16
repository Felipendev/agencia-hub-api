-- ADR 0008: coluna role → account_kind (linguagem ubíqua).
ALTER TABLE users RENAME COLUMN role TO account_kind;

ALTER INDEX IF EXISTS idx_users_role RENAME TO idx_users_account_kind;
