CREATE TABLE sales (
    id UUID PRIMARY KEY,
    agency_id UUID NOT NULL REFERENCES agencies(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES customers(id),
    quotation_id UUID REFERENCES quotations(id) ON DELETE SET NULL,
    total_amount NUMERIC(19, 2) NOT NULL,
    sale_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    notes TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_sales_agency_customer ON sales(agency_id, customer_id);

CREATE TABLE sale_items (
    id UUID PRIMARY KEY,
    sale_id UUID NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    supplier_id UUID REFERENCES suppliers(id) ON DELETE SET NULL,
    description VARCHAR(512) NOT NULL,
    item_type VARCHAR(32) NOT NULL,
    sale_amount NUMERIC(19, 2) NOT NULL,
    supplier_cost NUMERIC(19, 2),
    customer_pays_supplier_directly BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE receivables (
    id UUID PRIMARY KEY,
    sale_id UUID NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    installment_number INTEGER NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    received_at DATE,
    financial_entry_id UUID REFERENCES financial_entries(id) ON DELETE SET NULL,
    UNIQUE (sale_id, installment_number)
);

CREATE TABLE payables (
    id UUID PRIMARY KEY,
    sale_id UUID NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    supplier_id UUID REFERENCES suppliers(id) ON DELETE SET NULL,
    commission_recipient_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    payable_type VARCHAR(32) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    due_date DATE,
    status VARCHAR(32) NOT NULL,
    paid_at DATE,
    financial_entry_id UUID REFERENCES financial_entries(id) ON DELETE SET NULL
);

CREATE TABLE sale_commissions (
    id UUID PRIMARY KEY,
    sale_id UUID NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    recipient_user_id UUID NOT NULL REFERENCES users(id),
    calculation_type VARCHAR(16) NOT NULL,
    calculation_value NUMERIC(19, 4) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL
);
CREATE INDEX idx_sale_commissions_sale ON sale_commissions(sale_id);
