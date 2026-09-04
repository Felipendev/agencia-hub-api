-- Torna e-mail e destino de interesse OPCIONAIS em customers.
-- Necessário para cadastrar cliente que só tem milhas (faceta miles_clients),
-- que frequentemente não informa e-mail nem destino de viagem.
--
-- O índice único idx_customers_email_unique (lower(email)) NÃO muda: no Postgres,
-- NULL é distinto em índice único, então vários clientes sem e-mail coexistem e
-- e-mails preenchidos seguem únicos. As linhas existentes têm e-mail (era NOT NULL),
-- portanto nenhuma viola a constraint relaxada.

ALTER TABLE customers ALTER COLUMN email DROP NOT NULL;
ALTER TABLE customers ALTER COLUMN interest_destination DROP NOT NULL;
