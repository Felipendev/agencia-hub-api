CREATE TABLE agency_library_initialized (
 agency_id UUID PRIMARY KEY REFERENCES agencies(id) ON DELETE CASCADE,
 initialized_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE accumulation_simulations (
 id UUID PRIMARY KEY,
 agency_id UUID NOT NULL REFERENCES agencies(id) ON DELETE CASCADE,
 title VARCHAR(200) NOT NULL,
 assumptions JSONB NOT NULL,
 created_by VARCHAR(255) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX accumulation_simulations_agency_idx ON accumulation_simulations(agency_id, created_at DESC);

ALTER TABLE agency_library DROP CONSTRAINT agency_library_agency_id_fkey;
ALTER TABLE agency_library ADD CONSTRAINT agency_library_agency_id_fkey
 FOREIGN KEY (agency_id) REFERENCES agencies(id) ON DELETE CASCADE;
