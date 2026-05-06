-- V9__multi_tenancy_agencies.sql
-- Transforms AgenciaHub from single-tenant to multi-tenant architecture.

-- 1. Tabela de agências
CREATE TABLE agencies (
    id                  UUID         NOT NULL PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    phone               VARCHAR(32),
    logo_url            VARCHAR(1024),
    cnpj                VARCHAR(18),
    address             TEXT,
    commercial_email    VARCHAR(320),
    status              VARCHAR(32)  NOT NULL DEFAULT 'PENDING_VERIFICATION',
    subscription_status VARCHAR(32)  NOT NULL DEFAULT 'TRIAL',
    trial_ends_at       TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_agencies_status ON agencies (status);

-- 2. Adicionar agency_id e novos campos na tabela users
ALTER TABLE users ADD COLUMN agency_id UUID REFERENCES agencies (id);
ALTER TABLE users ADD COLUMN phone VARCHAR(32);
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN password_changed_at TIMESTAMP WITH TIME ZONE;

-- 3. Adicionar agency_id nas tabelas existentes
ALTER TABLE customers ADD COLUMN agency_id UUID REFERENCES agencies (id);
ALTER TABLE opportunities ADD COLUMN agency_id UUID REFERENCES agencies (id);
ALTER TABLE quotations ADD COLUMN agency_id UUID REFERENCES agencies (id);
ALTER TABLE financial_entries ADD COLUMN agency_id UUID REFERENCES agencies (id);

-- 4. Criar agência padrão para dados existentes
INSERT INTO agencies (id, name, status, subscription_status, created_at, updated_at)
VALUES (
    'a0000000-0000-0000-0000-000000000000',
    'Agência Padrão (Migração)',
    'ACTIVE',
    'ACTIVE',
    NOW(),
    NOW()
);

-- 5. Vincular dados existentes à agência padrão
UPDATE users SET agency_id = 'a0000000-0000-0000-0000-000000000000', email_verified = TRUE
WHERE agency_id IS NULL;

UPDATE customers SET agency_id = 'a0000000-0000-0000-0000-000000000000'
WHERE agency_id IS NULL;

UPDATE opportunities SET agency_id = 'a0000000-0000-0000-0000-000000000000'
WHERE agency_id IS NULL;

UPDATE quotations SET agency_id = 'a0000000-0000-0000-0000-000000000000'
WHERE agency_id IS NULL;

UPDATE financial_entries SET agency_id = 'a0000000-0000-0000-0000-000000000000'
WHERE agency_id IS NULL;

-- 6. Tornar agency_id NOT NULL após migração de dados
ALTER TABLE users ALTER COLUMN agency_id SET NOT NULL;
ALTER TABLE customers ALTER COLUMN agency_id SET NOT NULL;
ALTER TABLE opportunities ALTER COLUMN agency_id SET NOT NULL;
ALTER TABLE quotations ALTER COLUMN agency_id SET NOT NULL;
ALTER TABLE financial_entries ALTER COLUMN agency_id SET NOT NULL;

-- 7. Índices para multi-tenancy
CREATE INDEX idx_users_agency_id ON users (agency_id);
CREATE INDEX idx_customers_agency_id ON customers (agency_id);
CREATE INDEX idx_opportunities_agency_id ON opportunities (agency_id);
CREATE INDEX idx_quotations_agency_id ON quotations (agency_id);
CREATE INDEX idx_financial_entries_agency_id ON financial_entries (agency_id);

-- 8. Tabela de convites
CREATE TABLE invitations (
    id          UUID         NOT NULL PRIMARY KEY,
    agency_id   UUID         NOT NULL REFERENCES agencies (id),
    invited_by  UUID         NOT NULL REFERENCES users (id),
    email       VARCHAR(320) NOT NULL,
    token       VARCHAR(255) NOT NULL UNIQUE,
    status      VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_invitations_token ON invitations (token);
CREATE INDEX idx_invitations_agency_id ON invitations (agency_id);
CREATE INDEX idx_invitations_email ON invitations (email);

-- 9. Tabela de códigos de verificação
CREATE TABLE verification_codes (
    id          UUID         NOT NULL PRIMARY KEY,
    user_id     UUID         REFERENCES users (id),
    email       VARCHAR(320) NOT NULL,
    code_hash   VARCHAR(255) NOT NULL,
    type        VARCHAR(32)  NOT NULL,
    attempts    INT          NOT NULL DEFAULT 0,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_verification_codes_email ON verification_codes (email);
CREATE INDEX idx_verification_codes_user_id ON verification_codes (user_id);

-- 10. Tabela de aceite de termos
CREATE TABLE terms_acceptances (
    id             UUID         NOT NULL PRIMARY KEY,
    user_id        UUID         NOT NULL REFERENCES users (id),
    terms_version  VARCHAR(32)  NOT NULL,
    ip_address     VARCHAR(45),
    accepted_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_terms_acceptances_user_id ON terms_acceptances (user_id);

-- 11. Tabela de log de auditoria da agência
CREATE TABLE agency_audit_log (
    id          UUID         NOT NULL PRIMARY KEY,
    agency_id   UUID         NOT NULL REFERENCES agencies (id),
    user_id     UUID         NOT NULL REFERENCES users (id),
    action      VARCHAR(64)  NOT NULL,
    field_name  VARCHAR(128),
    old_value   TEXT,
    new_value   TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_agency_audit_log_agency_id ON agency_audit_log (agency_id);
