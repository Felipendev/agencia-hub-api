-- F03 — Economia realizada do cliente (TODO-047), produto Viagens.
-- Versão provisória: pressupõe a V56 da F02 integrada antes; o integrador define a numeração final.

-- Uma análise por viagem; o conteúdo editável fica em JSONB e cada edição gera uma versão imutável.
CREATE TABLE trip_savings (
 id UUID PRIMARY KEY,
 agency_id UUID NOT NULL REFERENCES agencies(id),
 trip_id UUID NOT NULL UNIQUE REFERENCES trips(id) ON DELETE CASCADE,
 data JSONB NOT NULL,
 version INTEGER NOT NULL DEFAULT 1 CHECK (version >= 1),
 created_by VARCHAR(255) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX trip_savings_agency_idx ON trip_savings(agency_id, updated_at DESC);

CREATE TABLE trip_savings_versions (
 id UUID PRIMARY KEY,
 savings_id UUID NOT NULL REFERENCES trip_savings(id) ON DELETE CASCADE,
 agency_id UUID NOT NULL REFERENCES agencies(id),
 version INTEGER NOT NULL CHECK (version >= 1),
 data JSONB NOT NULL,
 author VARCHAR(255) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE (savings_id, version)
);

-- Evidência do preço comparável (print, PDF da pesquisa).
CREATE TABLE trip_savings_evidence (
 id UUID PRIMARY KEY,
 savings_id UUID NOT NULL REFERENCES trip_savings(id) ON DELETE CASCADE,
 agency_id UUID NOT NULL REFERENCES agencies(id),
 description VARCHAR(500),
 filename VARCHAR(255) NOT NULL,
 content_type VARCHAR(150) NOT NULL,
 byte_size BIGINT NOT NULL CHECK (byte_size > 0),
 content BYTEA NOT NULL,
 author VARCHAR(255) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX trip_savings_evidence_idx ON trip_savings_evidence(savings_id, created_at);

-- Relatório compartilhável: retrato imutável depois das ocultações escolhidas pelo gestor.
CREATE TABLE trip_savings_reports (
 id UUID PRIMARY KEY,
 agency_id UUID NOT NULL REFERENCES agencies(id),
 customer_id UUID NOT NULL,
 period_from DATE,
 period_to DATE,
 options JSONB NOT NULL,
 content JSONB NOT NULL,
 author VARCHAR(255) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK (period_to IS NULL OR period_from IS NULL OR period_to >= period_from)
);
CREATE INDEX trip_savings_reports_idx ON trip_savings_reports(agency_id, customer_id, created_at DESC);

-- Auditoria própria; sem FK para preservar exclusões.
CREATE TABLE trip_savings_audit (
 id UUID PRIMARY KEY,
 agency_id UUID NOT NULL REFERENCES agencies(id),
 resource_type VARCHAR(16) NOT NULL CHECK (resource_type IN ('SAVINGS','REPORT')),
 resource_id UUID NOT NULL,
 trip_id UUID,
 customer_id UUID,
 action VARCHAR(40) NOT NULL,
 detail JSONB NOT NULL,
 author VARCHAR(255) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX trip_savings_audit_idx ON trip_savings_audit(agency_id, trip_id, created_at DESC);
