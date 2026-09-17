ALTER TABLE agency_attachments ADD COLUMN trip_id UUID REFERENCES trips(id) ON DELETE CASCADE;
ALTER TABLE agency_attachments DROP CONSTRAINT chk_agency_attachment_owner;
ALTER TABLE agency_attachments ADD CONSTRAINT chk_agency_attachment_owner
    CHECK ((sale_id IS NOT NULL)::int + (financial_entry_id IS NOT NULL)::int + (trip_id IS NOT NULL)::int = 1);
CREATE INDEX idx_agency_attachments_trip ON agency_attachments(trip_id);
