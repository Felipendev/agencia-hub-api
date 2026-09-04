CREATE TABLE agency_attachments (
    id UUID PRIMARY KEY,
    agency_id UUID NOT NULL REFERENCES agencies(id) ON DELETE CASCADE,
    sale_id UUID REFERENCES sales(id) ON DELETE CASCADE,
    financial_entry_id UUID REFERENCES financial_entries(id) ON DELETE CASCADE,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    byte_size BIGINT NOT NULL,
    content BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_agency_attachment_owner CHECK ((sale_id IS NOT NULL)::int + (financial_entry_id IS NOT NULL)::int = 1)
);
CREATE INDEX idx_agency_attachments_sale ON agency_attachments(sale_id);
CREATE INDEX idx_agency_attachments_financial_entry ON agency_attachments(financial_entry_id);
