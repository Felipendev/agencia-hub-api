-- Vincula cotação ao vendedor responsável pela comissão
ALTER TABLE quotations
    ADD COLUMN seller_id UUID REFERENCES users (id);

CREATE INDEX idx_quotations_seller_id ON quotations (seller_id)
    WHERE seller_id IS NOT NULL;
