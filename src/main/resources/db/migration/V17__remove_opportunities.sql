-- Remove opportunities aggregate: quotations no longer reference opportunities.
-- Em PostgreSQL, DROP COLUMN ... CASCADE remove a FK associada automaticamente.

ALTER TABLE quotations DROP COLUMN IF EXISTS opportunity_id CASCADE;

DROP INDEX IF EXISTS idx_quotations_opportunity_id;

DROP INDEX IF EXISTS idx_opportunities_agency_id;
DROP INDEX IF EXISTS idx_opportunities_customer_id;

DROP TABLE IF EXISTS opportunities;
