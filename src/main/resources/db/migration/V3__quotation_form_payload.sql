-- Structured form payload + CRM metadata (native JSON for Hibernate 6 SqlTypes.JSON).
ALTER TABLE quotations ADD COLUMN details_json JSON;

ALTER TABLE quotations ADD COLUMN tags JSON NOT NULL DEFAULT '[]';

ALTER TABLE quotations ADD COLUMN priority BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE quotations ADD COLUMN assignee VARCHAR(255);

ALTER TABLE quotations ADD COLUMN internal_notes TEXT NOT NULL DEFAULT '';
