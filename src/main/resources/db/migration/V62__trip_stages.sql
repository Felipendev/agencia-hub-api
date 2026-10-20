-- F10 (extensão) — Etapas da viagem (TODO-050), produto Viagens. V62 reservada para este módulo.
-- Hotel, transfer e atividades; trechos aéreos continuam em trip_segments.

CREATE TABLE trip_stages (
 id UUID PRIMARY KEY,
 agency_id UUID NOT NULL REFERENCES agencies(id),
 trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
 kind VARCHAR(10) NOT NULL CHECK (kind IN ('HOTEL','TRANSFER','ACTIVITY','OTHER')),
 title VARCHAR(180) NOT NULL,
 starts_at TIMESTAMPTZ NOT NULL,
 ends_at TIMESTAMPTZ,
 time_zone VARCHAR(64) NOT NULL DEFAULT 'America/Fortaleza',
 provider VARCHAR(200),
 confirmation_code VARCHAR(100),
 location VARCHAR(500),
 origin VARCHAR(300),
 destination VARCHAR(300),
 people INTEGER CHECK (people IS NULL OR people BETWEEN 1 AND 99),
 notes VARCHAR(4000),
 status VARCHAR(10) NOT NULL DEFAULT 'PLANNED' CHECK (status IN ('PLANNED','CONFIRMED','CANCELLED')),
 status_reason VARCHAR(1000),
 version INTEGER NOT NULL DEFAULT 1 CHECK (version >= 1),
 created_by VARCHAR(255) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK (ends_at IS NULL OR ends_at >= starts_at),
 -- Origem/destino só fazem sentido para transfer.
 CHECK (kind = 'TRANSFER' OR (origin IS NULL AND destination IS NULL))
);
CREATE INDEX trip_stages_trip_idx ON trip_stages(agency_id, trip_id, starts_at);

-- Vínculos, sem cópia: anexo da própria viagem ou material da Biblioteca da agência.
CREATE TABLE trip_stage_links (
 id UUID PRIMARY KEY,
 stage_id UUID NOT NULL REFERENCES trip_stages(id) ON DELETE CASCADE,
 agency_id UUID NOT NULL REFERENCES agencies(id),
 attachment_id UUID REFERENCES agency_attachments(id) ON DELETE CASCADE,
 library_id UUID REFERENCES agency_library(id) ON DELETE CASCADE,
 author VARCHAR(255) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK ((attachment_id IS NOT NULL)::int + (library_id IS NOT NULL)::int = 1),
 UNIQUE (stage_id, attachment_id),
 UNIQUE (stage_id, library_id)
);

-- Auditoria própria; sem FK para preservar o registro de exclusões.
CREATE TABLE trip_stage_events (
 id UUID PRIMARY KEY,
 agency_id UUID NOT NULL REFERENCES agencies(id),
 trip_id UUID NOT NULL,
 stage_id UUID NOT NULL,
 action VARCHAR(32) NOT NULL,
 detail JSONB NOT NULL,
 author VARCHAR(255) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX trip_stage_events_idx ON trip_stage_events(agency_id, trip_id, created_at DESC);
