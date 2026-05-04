CREATE TABLE quotations (
    id UUID NOT NULL PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers (id),
    opportunity_id UUID REFERENCES opportunities (id),
    title VARCHAR(512) NOT NULL,
    destination VARCHAR(512) NOT NULL,
    description TEXT NOT NULL,
    total_amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    status VARCHAR(32) NOT NULL,
    valid_until DATE NOT NULL,
    travel_start_date DATE,
    travel_end_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_quotations_customer_id ON quotations (customer_id);
CREATE INDEX idx_quotations_opportunity_id ON quotations (opportunity_id);
CREATE INDEX idx_quotations_status ON quotations (status);
CREATE INDEX idx_quotations_valid_until ON quotations (valid_until);
