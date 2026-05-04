CREATE TABLE customers (
    id UUID NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(320) NOT NULL,
    phone VARCHAR(64) NOT NULL,
    interest_destination VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL,
    notes TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Unicidade de e-mail (case-insensitive)
CREATE UNIQUE INDEX idx_customers_email_unique
    ON customers (lower(email));

-- Unicidade de telefone (apenas dígitos, ignora formatação)
CREATE UNIQUE INDEX idx_customers_phone_unique
    ON customers (regexp_replace(phone, '\D', '', 'g'))
    WHERE regexp_replace(phone, '\D', '', 'g') <> '';

CREATE TABLE opportunities (
    id UUID NOT NULL PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers (id),
    title VARCHAR(512) NOT NULL,
    destination VARCHAR(512) NOT NULL,
    estimated_amount NUMERIC(19, 2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    expected_travel_date DATE NOT NULL,
    notes TEXT NOT NULL
);

CREATE INDEX idx_opportunities_customer_id ON opportunities (customer_id);

CREATE TABLE financial_entries (
    id UUID NOT NULL PRIMARY KEY,
    description VARCHAR(1024) NOT NULL,
    type VARCHAR(32) NOT NULL,
    category VARCHAR(32) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    entry_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    customer_id UUID REFERENCES customers (id)
);

CREATE INDEX idx_financial_entries_customer_id ON financial_entries (customer_id);
CREATE INDEX idx_financial_entries_entry_date ON financial_entries (entry_date);
