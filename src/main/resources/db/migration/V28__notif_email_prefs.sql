ALTER TABLE users
    ADD COLUMN IF NOT EXISTS notif_email_cotacao_aprovada   BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS notif_email_cotacao_vencendo   BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS notif_email_cotacao_vencida    BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS notif_email_exclusao_agendada  BOOLEAN NOT NULL DEFAULT TRUE;
