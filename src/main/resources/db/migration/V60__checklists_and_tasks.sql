-- F01/Codex. V59 pertence a F07/Claude: integrar e publicar V59 antes desta migration.
CREATE TABLE checklist_templates (
 id UUID PRIMARY KEY, agency_id UUID NOT NULL REFERENCES agencies(id) ON DELETE CASCADE,
 product VARCHAR(6) NOT NULL CHECK(product IN ('TRAVEL','MILES')),
 name VARCHAR(180) NOT NULL, frequency VARCHAR(7) NOT NULL CHECK(frequency IN ('ONCE','WEEKLY','MONTHLY')),
 steps JSONB NOT NULL, version INT NOT NULL DEFAULT 1 CHECK(version>0),
 deleted_at TIMESTAMPTZ, updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX checklist_templates_agency_idx ON checklist_templates(agency_id,product);
CREATE TABLE checklist_runs (
 id UUID PRIMARY KEY, agency_id UUID NOT NULL REFERENCES agencies(id) ON DELETE CASCADE,
 template_id UUID NOT NULL REFERENCES checklist_templates(id),
 customer_id UUID NOT NULL REFERENCES customers(id), product VARCHAR(6) NOT NULL CHECK(product IN ('TRAVEL','MILES')),
 title VARCHAR(180) NOT NULL, notes VARCHAR(4000) NOT NULL DEFAULT '',
 period_key VARCHAR(16) NOT NULL, period_start DATE NOT NULL, period_end DATE NOT NULL,
 snapshot JSONB NOT NULL, version INT NOT NULL DEFAULT 1 CHECK(version>0), cancelled_at TIMESTAMPTZ,
 cancel_reason VARCHAR(1000) NOT NULL DEFAULT '',
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE(agency_id,template_id,customer_id,period_key)
);
CREATE INDEX checklist_runs_agency_idx ON checklist_runs(agency_id,product,customer_id);
CREATE TABLE work_tasks (
 id UUID PRIMARY KEY, agency_id UUID NOT NULL REFERENCES agencies(id) ON DELETE CASCADE,
 customer_id UUID NOT NULL REFERENCES customers(id), product VARCHAR(6) NOT NULL CHECK(product IN ('TRAVEL','MILES')),
 title VARCHAR(180) NOT NULL, notes VARCHAR(4000) NOT NULL DEFAULT '', due_on DATE NOT NULL,
 responsible_id UUID REFERENCES users(id), trip_id UUID REFERENCES trips(id), quotation_id UUID REFERENCES quotations(id),
 run_id UUID REFERENCES checklist_runs(id), creation_key UUID NOT NULL, creation_payload JSONB NOT NULL,
 state VARCHAR(9) NOT NULL DEFAULT 'OPEN' CHECK(state IN ('OPEN','DONE','CANCELLED')),
 version INT NOT NULL DEFAULT 1 CHECK(version>0), deleted_at TIMESTAMPTZ,
 state_by UUID, state_at TIMESTAMPTZ, state_reason VARCHAR(1000) NOT NULL DEFAULT '',
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE(agency_id,creation_key),
 CHECK(product='TRAVEL' OR (trip_id IS NULL AND quotation_id IS NULL))
);
CREATE INDEX work_tasks_agency_due_idx ON work_tasks(agency_id,product,state,due_on) WHERE deleted_at IS NULL;
CREATE INDEX work_tasks_run_idx ON work_tasks(agency_id,run_id);
CREATE TABLE work_task_events (
 id UUID PRIMARY KEY, agency_id UUID NOT NULL REFERENCES agencies(id) ON DELETE CASCADE,
 resource_type VARCHAR(8) NOT NULL CHECK(resource_type IN ('TASK','TEMPLATE','RUN')),
 resource_id UUID NOT NULL, customer_id UUID REFERENCES customers(id),
 action VARCHAR(24) NOT NULL, snapshot JSONB NOT NULL, author_id UUID NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX work_task_events_resource_idx ON work_task_events(agency_id,resource_type,resource_id,created_at);
CREATE INDEX work_task_events_customer_idx ON work_task_events(agency_id,customer_id);
