ALTER TABLE quotations ADD COLUMN IF NOT EXISTS flight_plan jsonb;

CREATE TABLE IF NOT EXISTS quotation_flight_history (
    id uuid PRIMARY KEY,
    quotation_id uuid NOT NULL REFERENCES quotations(id) ON DELETE CASCADE,
    agency_id uuid NOT NULL REFERENCES agencies(id) ON DELETE CASCADE,
    saved_at timestamptz NOT NULL DEFAULT now(),
    saved_by uuid,
    plan jsonb NOT NULL
);
CREATE INDEX IF NOT EXISTS quotation_flight_history_lookup ON quotation_flight_history(agency_id, quotation_id, saved_at DESC);

CREATE TABLE IF NOT EXISTS flight_import_usage (
    agency_id uuid NOT NULL REFERENCES agencies(id) ON DELETE CASCADE,
    period date NOT NULL,
    attempts integer NOT NULL DEFAULT 0,
    charged_usd numeric(14,6) NOT NULL DEFAULT 0,
    PRIMARY KEY (agency_id, period)
);
CREATE TABLE IF NOT EXISTS flight_imports (
    id uuid PRIMARY KEY,
    agency_id uuid NOT NULL REFERENCES agencies(id) ON DELETE CASCADE,
    file_hash varchar(64) NOT NULL,
    extractor_version varchar(100) NOT NULL,
    filename varchar(255) NOT NULL,
    status varchar(20) NOT NULL,
    attempt_id uuid NOT NULL,
    result jsonb,
    created_by uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    input_tokens integer NOT NULL DEFAULT 0,
    output_tokens integer NOT NULL DEFAULT 0,
    estimated_usd numeric(14,6) NOT NULL DEFAULT 0,
    UNIQUE (agency_id, file_hash, extractor_version)
);
