ALTER TABLE accumulation_simulations ADD COLUMN version INTEGER NOT NULL DEFAULT 1;
ALTER TABLE accumulation_simulations ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE accumulation_simulations ADD COLUMN updated_by VARCHAR(255);
UPDATE accumulation_simulations SET updated_at=created_at,updated_by=created_by;

CREATE TABLE accumulation_simulation_versions (
 id UUID PRIMARY KEY,
 simulation_id UUID NOT NULL REFERENCES accumulation_simulations(id) ON DELETE CASCADE,
 version INTEGER NOT NULL,
 assumptions JSONB NOT NULL,
 author VARCHAR(255) NOT NULL,
 recorded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE(simulation_id,version)
);
INSERT INTO accumulation_simulation_versions(id,simulation_id,version,assumptions,author,recorded_at)
 SELECT gen_random_uuid(),id,1,assumptions,created_by,created_at FROM accumulation_simulations;
