-- Aceite de termos passa a ser flag em users; remove tabela legada.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS terms_accepted BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE users u
SET terms_accepted = TRUE
WHERE EXISTS (SELECT 1 FROM terms_acceptances t WHERE t.user_id = u.id);

DROP TABLE IF EXISTS terms_acceptances;
