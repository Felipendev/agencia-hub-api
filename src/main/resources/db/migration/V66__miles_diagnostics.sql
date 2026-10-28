CREATE TABLE miles_diagnostic_configs (
    agency_id UUID PRIMARY KEY REFERENCES agencies(id) ON DELETE CASCADE,
    token UUID NOT NULL UNIQUE,
    title VARCHAR(160) NOT NULL,
    introduction VARCHAR(1000) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT false,
    version INTEGER NOT NULL DEFAULT 1 CHECK(version>0)
);
CREATE TABLE miles_diagnostics (
    id UUID PRIMARY KEY,
    agency_id UUID NOT NULL REFERENCES agencies(id) ON DELETE CASCADE,
    request_key UUID NOT NULL,
    original_data JSONB NOT NULL,
    data JSONB NOT NULL,
    source VARCHAR(8) NOT NULL CHECK(source IN ('PUBLIC','STAFF')),
    consent_version VARCHAR(40),
    version INTEGER NOT NULL DEFAULT 1 CHECK(version>0),
    archived BOOLEAN NOT NULL DEFAULT false,
    customer_id UUID REFERENCES customers(id),
    applied_input JSONB,
    receipt JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(agency_id,request_key),
    CHECK ((receipt IS NULL AND applied_input IS NULL AND customer_id IS NULL) OR (receipt IS NOT NULL AND applied_input IS NOT NULL AND customer_id IS NOT NULL)),
    CHECK(source<>'PUBLIC' OR consent_version IS NOT NULL)
);
CREATE INDEX miles_diagnostics_agency_date ON miles_diagnostics(agency_id,created_at DESC,id);
CREATE INDEX miles_diagnostics_customer ON miles_diagnostics(agency_id,customer_id);
CREATE TABLE miles_diagnostic_events (
    id UUID PRIMARY KEY,
    agency_id UUID NOT NULL REFERENCES agencies(id) ON DELETE CASCADE,
    diagnostic_id UUID NOT NULL REFERENCES miles_diagnostics(id) ON DELETE CASCADE,
    action VARCHAR(32) NOT NULL,
    author_id UUID,
    reason VARCHAR(500) NOT NULL DEFAULT '',
    snapshot JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX miles_diagnostic_events_origin ON miles_diagnostic_events(agency_id,diagnostic_id,created_at,id);
CREATE FUNCTION check_miles_diagnostic_tenant() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF TG_TABLE_NAME='miles_diagnostics' THEN
    IF NEW.customer_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM customers WHERE id=NEW.customer_id AND agency_id=NEW.agency_id) THEN
      RAISE EXCEPTION 'Pessoa do diagnóstico deve pertencer à mesma agência';
    END IF;
  ELSIF NOT EXISTS(SELECT 1 FROM miles_diagnostics WHERE id=NEW.diagnostic_id AND agency_id=NEW.agency_id) THEN
    RAISE EXCEPTION 'Histórico do diagnóstico deve pertencer à mesma agência';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER miles_diagnostic_tenant BEFORE INSERT OR UPDATE ON miles_diagnostics FOR EACH ROW EXECUTE FUNCTION check_miles_diagnostic_tenant();
CREATE TRIGGER miles_diagnostic_event_tenant BEFORE INSERT OR UPDATE ON miles_diagnostic_events FOR EACH ROW EXECUTE FUNCTION check_miles_diagnostic_tenant();
