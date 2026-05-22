-- LGPD-02: solicitações de exclusão de dados por titulares externos (art. 18, VI)

CREATE TABLE IF NOT EXISTS data_deletion_requests (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(320) NOT NULL,
    telefone        VARCHAR(32),
    motivo          TEXT,
    status          VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','PROCESSED','REJECTED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at    TIMESTAMPTZ,
    processed_by_id UUID        REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_data_deletion_requests_email ON data_deletion_requests(email);
CREATE INDEX IF NOT EXISTS idx_data_deletion_requests_status ON data_deletion_requests(status);
