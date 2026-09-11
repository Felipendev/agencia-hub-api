-- Existing destination values remain available as legacy history.
-- NULL profile_data identifies records created before full CRM profile persistence.
ALTER TABLE customers ADD COLUMN profile_data JSONB;
ALTER TABLE customers ADD CONSTRAINT customers_profile_data_object
    CHECK (profile_data IS NULL OR jsonb_typeof(profile_data) = 'object');
