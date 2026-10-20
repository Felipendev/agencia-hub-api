-- F07 — Agenda e atas de atendimento (TODO-048). V59 reservada para este módulo.
-- Viagens e Milhas são produtos distintos: todo compromisso registra o produto.

CREATE TABLE appointments (
 id UUID PRIMARY KEY,
 agency_id UUID NOT NULL REFERENCES agencies(id),
 customer_id UUID NOT NULL REFERENCES customers(id),
 product VARCHAR(8) NOT NULL CHECK (product IN ('TRAVEL','MILES')),
 trip_id UUID REFERENCES trips(id) ON DELETE SET NULL,
 title VARCHAR(180) NOT NULL,
 purpose VARCHAR(2000),
 starts_at TIMESTAMPTZ NOT NULL,
 duration_minutes INTEGER NOT NULL CHECK (duration_minutes BETWEEN 5 AND 720),
 time_zone VARCHAR(64) NOT NULL DEFAULT 'America/Fortaleza',
 format VARCHAR(12) NOT NULL CHECK (format IN ('IN_PERSON','VIDEO','PHONE','OTHER')),
 location VARCHAR(300),
 meeting_url VARCHAR(2048),
 responsible_id UUID NOT NULL REFERENCES users(id),
 status VARCHAR(12) NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED','COMPLETED','NO_SHOW','CANCELLED')),
 status_reason VARCHAR(1000),
 status_changed_at TIMESTAMPTZ,
 version INTEGER NOT NULL DEFAULT 1 CHECK (version >= 1),
 created_by VARCHAR(255) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 -- Viagem só se vincula a atendimento de Viagens.
 CHECK (product = 'TRAVEL' OR trip_id IS NULL)
);
CREATE INDEX appointments_agency_start_idx ON appointments(agency_id, starts_at);
CREATE INDEX appointments_agency_customer_idx ON appointments(agency_id, customer_id, starts_at DESC);
CREATE INDEX appointments_responsible_idx ON appointments(agency_id, responsible_id, starts_at);

-- Ata: uma por compromisso; cada edição gera versão imutável.
CREATE TABLE appointment_minutes (
 appointment_id UUID PRIMARY KEY REFERENCES appointments(id) ON DELETE CASCADE,
 agency_id UUID NOT NULL REFERENCES agencies(id),
 data JSONB NOT NULL,
 search_text TEXT NOT NULL,
 version INTEGER NOT NULL DEFAULT 1 CHECK (version >= 1),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE appointment_minutes_versions (
 id UUID PRIMARY KEY,
 appointment_id UUID NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
 agency_id UUID NOT NULL REFERENCES agencies(id),
 version INTEGER NOT NULL CHECK (version >= 1),
 data JSONB NOT NULL,
 author VARCHAR(255) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE (appointment_id, version)
);

-- Auditoria própria; sem FK para preservar o registro de exclusões.
CREATE TABLE appointment_events (
 id UUID PRIMARY KEY,
 agency_id UUID NOT NULL REFERENCES agencies(id),
 appointment_id UUID NOT NULL,
 customer_id UUID,
 action VARCHAR(32) NOT NULL,
 detail JSONB NOT NULL,
 author VARCHAR(255) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX appointment_events_idx ON appointment_events(agency_id, appointment_id, created_at DESC);
