CREATE TABLE customer_miles_profiles (
 customer_id UUID PRIMARY KEY REFERENCES customers(id) ON DELETE CASCADE,
 agency_id UUID NOT NULL REFERENCES agencies(id),
 data JSONB NOT NULL,
 version INTEGER NOT NULL DEFAULT 1,
 archived BOOLEAN NOT NULL DEFAULT false,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE customer_miles_items (
 id UUID PRIMARY KEY,
 customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
 agency_id UUID NOT NULL REFERENCES agencies(id),
 kind VARCHAR(16) NOT NULL CHECK(kind IN ('ACCOUNT','CARD','CLUB','EXPIRY')),
 data JSONB NOT NULL,
 version INTEGER NOT NULL DEFAULT 1,
 archived BOOLEAN NOT NULL DEFAULT false,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX customer_miles_items_scope ON customer_miles_items(agency_id,customer_id);
CREATE TABLE customer_miles_movements (
 id UUID PRIMARY KEY,
 account_id UUID NOT NULL REFERENCES customer_miles_items(id),
 quantity BIGINT NOT NULL CHECK(quantity <> 0),
 reason VARCHAR(500) NOT NULL,
 occurred_on DATE NOT NULL,
 request_key UUID NOT NULL,
 reverses_id UUID REFERENCES customer_miles_movements(id),
 author VARCHAR(255) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE(account_id,request_key),
 UNIQUE(reverses_id)
);
CREATE INDEX customer_miles_movements_account ON customer_miles_movements(account_id,created_at);
CREATE TABLE customer_miles_audit (
 id UUID PRIMARY KEY,
 customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
 agency_id UUID NOT NULL REFERENCES agencies(id),
 resource_id UUID NOT NULL,
 action VARCHAR(32) NOT NULL,
 snapshot JSONB NOT NULL,
 author VARCHAR(255) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX customer_miles_audit_scope ON customer_miles_audit(agency_id,customer_id,created_at DESC);
