-- Código curto por usuário para links públicos (?vendedor=...).
-- Sem geração de valores em SQL: o preenchimento é feito em Java (PublicLinkCodeService),
-- tipicamente no primeiro login após o deploy.
ALTER TABLE users ADD COLUMN IF NOT EXISTS public_link_code VARCHAR(16);

-- Unicidade apenas quando preenchido (permite NULL até o backfill em aplicação).
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_public_link_code ON users (public_link_code)
    WHERE public_link_code IS NOT NULL;