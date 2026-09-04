CREATE TABLE suppliers (
    id UUID PRIMARY KEY,
    agency_id UUID NOT NULL REFERENCES agencies(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    contact_name VARCHAR(255),
    email VARCHAR(320),
    phone VARCHAR(64),
    notes TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_suppliers_agency_name UNIQUE (agency_id, name)
);

CREATE INDEX idx_suppliers_agency_id ON suppliers(agency_id);

ALTER TABLE financial_entries
    ADD COLUMN supplier_id UUID REFERENCES suppliers(id) ON DELETE SET NULL,
    ADD COLUMN notes TEXT,
    ADD COLUMN sale_amount NUMERIC(19, 2),
    ADD COLUMN supplier_cost NUMERIC(19, 2),
    ADD COLUMN commission_amount NUMERIC(19, 2);

CREATE INDEX idx_financial_entries_supplier_id ON financial_entries(supplier_id);
