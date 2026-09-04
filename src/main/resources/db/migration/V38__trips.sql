CREATE TABLE trips (
    id UUID PRIMARY KEY,
    agency_id UUID NOT NULL REFERENCES agencies(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES customers(id),
    supplier_id UUID REFERENCES suppliers(id) ON DELETE SET NULL,
    quotation_id UUID REFERENCES quotations(id) ON DELETE SET NULL,
    sale_id UUID REFERENCES sales(id) ON DELETE SET NULL,
    service_type VARCHAR(32) NOT NULL,
    booking_locator VARCHAR(64),
    airline VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    sale_date DATE,
    travel_start_date DATE,
    travel_end_date DATE,
    notes TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_trips_agency_customer ON trips(agency_id, customer_id);
CREATE INDEX idx_trips_agency_locator ON trips(agency_id, booking_locator);

CREATE TABLE trip_segments (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    segment_number INTEGER NOT NULL,
    origin VARCHAR(128),
    destination VARCHAR(128),
    departure_at TIMESTAMPTZ,
    arrival_at TIMESTAMPTZ,
    airline VARCHAR(128),
    flight_number VARCHAR(32),
    ticket_number VARCHAR(64),
    UNIQUE (trip_id, segment_number)
);
