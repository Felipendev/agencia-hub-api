-- TODO-035: desconto percentual com teto opcional, limite de uso total e 1x por cliente (por e-mail).
ALTER TABLE coupons ADD COLUMN discount_percent NUMERIC(5,2) CHECK (discount_percent IS NULL OR (discount_percent > 0 AND discount_percent <= 100));
ALTER TABLE coupons ADD COLUMN max_discount_amount NUMERIC(19,2) CHECK (max_discount_amount IS NULL OR max_discount_amount > 0);
ALTER TABLE coupons ADD COLUMN max_uses INTEGER CHECK (max_uses IS NULL OR max_uses > 0);
ALTER TABLE coupons ADD COLUMN used_count INTEGER NOT NULL DEFAULT 0;

-- Um resgate por cupom+cliente (e-mail normalizado em minúsculas) — impede reuso pelo mesmo cliente.
CREATE TABLE coupon_redemptions (
    id             UUID PRIMARY KEY,
    coupon_id      UUID NOT NULL REFERENCES coupons(id) ON DELETE CASCADE,
    agency_id      UUID NOT NULL REFERENCES agencies(id) ON DELETE CASCADE,
    customer_email VARCHAR(320) NOT NULL,
    submission_id  UUID,
    redeemed_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX uq_coupon_redemptions_coupon_email ON coupon_redemptions(coupon_id, customer_email);
CREATE INDEX idx_coupon_redemptions_agency ON coupon_redemptions(agency_id);
