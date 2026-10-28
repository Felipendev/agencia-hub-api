-- F08/Codex; V64 reservada no registro central. Não migra tarefas nem altera o CRM.
CREATE TABLE commercial_followups (
 id UUID PRIMARY KEY, agency_id UUID NOT NULL REFERENCES agencies(id),
 customer_id UUID NOT NULL REFERENCES customers(id),
 product VARCHAR(6) NOT NULL CHECK (product IN ('TRAVEL','MILES')),
 title VARCHAR(180) NOT NULL CHECK (length(trim(title)) > 0),
 notes VARCHAR(4000) NOT NULL DEFAULT '', due_on DATE NOT NULL,
 responsible_id UUID NOT NULL REFERENCES users(id),
 state VARCHAR(9) NOT NULL DEFAULT 'OPEN' CHECK (state IN ('OPEN','DONE','CANCELLED')),
 state_reason VARCHAR(1000) NOT NULL DEFAULT '',
 version INTEGER NOT NULL DEFAULT 1 CHECK (version > 0),
 creation_key UUID NOT NULL, creation_payload JSONB NOT NULL,
 deleted_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), UNIQUE(agency_id,creation_key)
);
CREATE INDEX commercial_followups_due_idx ON commercial_followups(agency_id,product,state,due_on,id) WHERE deleted_at IS NULL;
CREATE INDEX commercial_followups_customer_idx ON commercial_followups(agency_id,customer_id);
CREATE INDEX commercial_followups_responsible_idx ON commercial_followups(agency_id,responsible_id);
CREATE TABLE commercial_followup_events (
 id UUID PRIMARY KEY, agency_id UUID NOT NULL REFERENCES agencies(id),
 followup_id UUID NOT NULL, customer_id UUID NOT NULL REFERENCES customers(id),
 action VARCHAR(24) NOT NULL, snapshot JSONB NOT NULL,
 author_id UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX commercial_followup_events_resource_idx ON commercial_followup_events(agency_id,followup_id,created_at,id);
CREATE INDEX commercial_followup_events_customer_idx ON commercial_followup_events(agency_id,customer_id);
