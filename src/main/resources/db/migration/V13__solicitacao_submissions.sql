-- Public quotation form submissions (persisted per agency when slug matches solicitacao_configs).

CREATE TABLE solicitacao_submissions (
    id           UUID         NOT NULL PRIMARY KEY,
    agency_id    UUID         REFERENCES agencies (id),
    slug         VARCHAR(128) NOT NULL,
    nome         VARCHAR(255) NOT NULL,
    email        VARCHAR(320) NOT NULL DEFAULT '',
    telefone     VARCHAR(32)  NOT NULL,
    observacoes  TEXT         NOT NULL DEFAULT '',
    detalhes     JSONB        NOT NULL DEFAULT '{}',
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_solicitacao_submissions_agency_created
    ON solicitacao_submissions (agency_id, created_at DESC);

CREATE INDEX idx_solicitacao_submissions_slug
    ON solicitacao_submissions (slug);
