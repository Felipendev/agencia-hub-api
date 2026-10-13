CREATE TABLE agency_library (
 id UUID PRIMARY KEY,
 agency_id UUID NOT NULL REFERENCES agencies(id),
 title VARCHAR(200) NOT NULL,
 category VARCHAR(32) NOT NULL,
 kind VARCHAR(16) NOT NULL CHECK (kind IN ('LINK','FILE','TEMPLATE')),
 url VARCHAR(2048),
 body TEXT,
 filename VARCHAR(255),
 content_type VARCHAR(150),
 byte_size BIGINT,
 content BYTEA,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX agency_library_agency_idx ON agency_library(agency_id, title);
