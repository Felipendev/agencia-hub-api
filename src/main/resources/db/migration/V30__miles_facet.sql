-- Faceta de milhas do cliente (DD-4): tabelas próprias com FK para customers e
-- agency_id denormalizado (multi-tenancy, mesmo padrão de quotations). Nenhuma
-- coluna de milhas dentro de customers.
--
-- miles_clients: 0..1 por cliente (UNIQUE em customer_id). Uma pessoa vira
-- "cliente de milhas" ao ganhar uma linha aqui, como já vira "de viagem" ao
-- ganhar uma quotation.
-- miles_cards: 0..N cartões de acúmulo por faceta (a pessoa acumula em vários
-- cartões). ON DELETE CASCADE — remover a faceta remove os cartões.

CREATE TABLE miles_clients (
    id                 UUID        NOT NULL PRIMARY KEY,
    agency_id          UUID        NOT NULL REFERENCES agencies (id),
    customer_id        UUID        NOT NULL REFERENCES customers (id),
    status             VARCHAR(32) NOT NULL,
    contract_signed_at DATE,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_miles_clients_customer UNIQUE (customer_id)
);

CREATE INDEX idx_miles_clients_agency_id ON miles_clients (agency_id);

CREATE TABLE miles_cards (
    id                UUID          NOT NULL PRIMARY KEY,
    agency_id         UUID          NOT NULL REFERENCES agencies (id),
    miles_client_id   UUID          NOT NULL REFERENCES miles_clients (id) ON DELETE CASCADE,
    bank              VARCHAR(128)  NOT NULL,
    points_per_dollar NUMERIC(10, 4) NOT NULL,
    monthly_spend     NUMERIC(19, 2) NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_miles_cards_miles_client_id ON miles_cards (miles_client_id);
CREATE INDEX idx_miles_cards_agency_id ON miles_cards (agency_id);
