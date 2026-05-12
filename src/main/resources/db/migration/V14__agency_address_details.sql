-- Structured address fields for the agency (tab "Endereço" in the app).

ALTER TABLE agencies ADD COLUMN IF NOT EXISTS address_details JSONB;
