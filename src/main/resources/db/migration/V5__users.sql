-- Sistema de usuários com roles OWNER e SELLER
CREATE TABLE users (
    id          UUID         NOT NULL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role        VARCHAR(16)  NOT NULL,          -- OWNER | SELLER
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    -- Comissão do vendedor: percentual OU valor fixo (apenas um deve ser preenchido)
    commission_pct   NUMERIC(5, 2),             -- ex: 5.00 = 5%
    commission_fixed NUMERIC(19, 2),            -- ex: 200.00 = R$ 200 fixo por cotação aprovada
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_users_email  ON users (email);
CREATE INDEX idx_users_role   ON users (role);
CREATE INDEX idx_users_active ON users (active);
