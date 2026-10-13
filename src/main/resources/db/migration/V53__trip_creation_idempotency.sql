ALTER TABLE trips ADD COLUMN creation_request_id UUID;
ALTER TABLE trips ADD COLUMN creation_request_hash VARCHAR(64);
CREATE UNIQUE INDEX trips_agency_creation_request_unique ON trips(agency_id, creation_request_id)
 WHERE creation_request_id IS NOT NULL;
