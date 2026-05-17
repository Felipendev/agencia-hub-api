ALTER TABLE solicitacao_submissions
    ADD COLUMN IF NOT EXISTS consentimento_lgpd BOOLEAN NOT NULL DEFAULT FALSE;
