-- SEC-03: auditoria de consentimento LGPD
-- Campos de rastreabilidade adicionados à tabela de submissões

ALTER TABLE solicitacao_submissions
    ADD COLUMN IF NOT EXISTS consentimento_at         TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS consentimento_ip         VARCHAR(45),
    ADD COLUMN IF NOT EXISTS consentimento_versao_termos VARCHAR(32);

-- Log de consentimento e revogações

CREATE TABLE IF NOT EXISTS consentimento_log (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id   UUID        REFERENCES solicitacao_submissions(id) ON DELETE CASCADE,
    tipo            VARCHAR(16) NOT NULL CHECK (tipo IN ('CONSENTIMENTO', 'REVOGACAO')),
    ip              VARCHAR(45),
    versao_termos   VARCHAR(32),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_consentimento_log_submission_id ON consentimento_log(submission_id);
