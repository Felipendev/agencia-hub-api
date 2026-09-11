CREATE TABLE agency_checkin_prefs (
    agency_id UUID PRIMARY KEY REFERENCES agencies(id) ON DELETE CASCADE,
    start_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    start_days INTEGER NOT NULL DEFAULT 2 CHECK (start_days BETWEEN 1 AND 30),
    end_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    end_days INTEGER NOT NULL DEFAULT 2 CHECK (end_days BETWEEN 1 AND 30),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE agency_checkin_notifications (
    id UUID PRIMARY KEY,
    agency_id UUID NOT NULL REFERENCES agencies(id) ON DELETE CASCADE,
    event_key VARCHAR(150) NOT NULL,
    event_date DATE NOT NULL,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    link TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    read_at TIMESTAMPTZ,
    dismissed_at TIMESTAMPTZ,
    UNIQUE (agency_id, event_key)
);
CREATE INDEX idx_checkin_inbox ON agency_checkin_notifications(agency_id, created_at DESC)
    WHERE dismissed_at IS NULL;
