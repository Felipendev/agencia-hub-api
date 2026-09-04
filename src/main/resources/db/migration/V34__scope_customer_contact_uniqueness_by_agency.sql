-- O mesmo cliente pode existir em agências diferentes. E-mail e telefone só
-- precisam ser únicos dentro da agência à qual o cadastro pertence.
DROP INDEX IF EXISTS idx_customers_email_unique;
DROP INDEX IF EXISTS idx_customers_phone_unique;

CREATE UNIQUE INDEX idx_customers_agency_email_unique
    ON customers (agency_id, lower(email))
    WHERE email IS NOT NULL;

CREATE UNIQUE INDEX idx_customers_agency_phone_unique
    ON customers (agency_id, regexp_replace(phone, '\\D', '', 'g'))
    WHERE phone IS NOT NULL AND regexp_replace(phone, '\\D', '', 'g') <> '';
