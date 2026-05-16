-- Alinha valores de role com AccountKind (ADR 0008).
UPDATE users SET role = 'AGENCY_OWNER' WHERE role = 'OWNER';
UPDATE users SET role = 'SALES_AGENT' WHERE role = 'SELLER';
