CREATE TABLE coupons (
    id          UUID PRIMARY KEY,
    agency_id   UUID NOT NULL REFERENCES agencies(id) ON DELETE CASCADE,
    code        VARCHAR(40) NOT NULL,
    expires_at  TIMESTAMPTZ,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- One code per agency, case-insensitive (entity always upper-cases before persisting).
CREATE UNIQUE INDEX uq_coupons_agency_code ON coupons(agency_id, code);
CREATE INDEX idx_coupons_agency ON coupons(agency_id);
