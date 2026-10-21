-- F09 — Indicações (TODO-049), cadastro e acompanhamento indicador → indicado. V63 reservada para este módulo.
-- Sem créditos, descontos ou lançamentos: essas regras ainda não foram definidas.

CREATE TABLE referrals (
 id UUID PRIMARY KEY,
 agency_id UUID NOT NULL REFERENCES agencies(id),
 product VARCHAR(10) NOT NULL CHECK (product IN ('TRAVEL','MILES')),
 -- Sem cascata: excluir uma pessoa que participa de indicações exige tratar as indicações antes.
 referrer_customer_id UUID NOT NULL REFERENCES customers(id),
 referred_customer_id UUID REFERENCES customers(id),
 -- Contato de quem ainda não está no CRM; limpo ao vincular (o valor anterior fica na auditoria).
 referred_name VARCHAR(200),
 referred_phone VARCHAR(25),
 referred_phone_key VARCHAR(8),
 referred_email VARCHAR(254),
 referred_on DATE NOT NULL,
 channel VARCHAR(16) NOT NULL CHECK (channel IN ('CONVERSATION','WHATSAPP','SOCIAL_MEDIA','EVENT','OTHER')),
 channel_detail VARCHAR(200),
 responsible_id UUID REFERENCES users(id),
 notes VARCHAR(4000),
 status VARCHAR(16) NOT NULL DEFAULT 'RECEIVED' CHECK (status IN ('RECEIVED','CONTACTED','CONVERTED','NOT_CONVERTED','CANCELLED')),
 status_reason VARCHAR(1000),
 converted_on DATE,
 -- Evidência opcional da conversão em Viagens; excluir a venda remove só o vínculo.
 sale_id UUID REFERENCES sales(id) ON DELETE SET NULL,
 -- Uma indicação que já foi convertida é cancelada, nunca excluída.
 ever_converted BOOLEAN NOT NULL DEFAULT false,
 version INTEGER NOT NULL DEFAULT 1 CHECK (version >= 1),
 created_by VARCHAR(255) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK (referred_customer_id IS NOT NULL OR (referred_name IS NOT NULL AND (referred_phone IS NOT NULL OR referred_email IS NOT NULL))),
 CHECK (referred_customer_id IS NULL OR referred_customer_id <> referrer_customer_id),
 CHECK (channel <> 'OTHER' OR channel_detail IS NOT NULL),
 CHECK (status <> 'CONVERTED' OR (referred_customer_id IS NOT NULL AND converted_on IS NOT NULL)),
 CHECK (converted_on IS NULL OR converted_on >= referred_on),
 CHECK (sale_id IS NULL OR product = 'TRAVEL')
);
-- Uma pessoa do CRM tem no máximo uma indicação ativa por produto.
CREATE UNIQUE INDEX referrals_active_referred_uq ON referrals(agency_id, product, referred_customer_id)
 WHERE referred_customer_id IS NOT NULL AND status <> 'CANCELLED';
CREATE INDEX referrals_list_idx ON referrals(agency_id, product, referred_on DESC);
CREATE INDEX referrals_referrer_idx ON referrals(agency_id, referrer_customer_id);
CREATE INDEX referrals_referred_idx ON referrals(agency_id, referred_customer_id);
CREATE INDEX referrals_contact_idx ON referrals(agency_id, referred_phone_key);

-- Auditoria própria; sem FK de indicação para preservar o registro de exclusões.
CREATE TABLE referral_events (
 id UUID PRIMARY KEY,
 agency_id UUID NOT NULL REFERENCES agencies(id),
 referral_id UUID NOT NULL,
 action VARCHAR(32) NOT NULL,
 detail JSONB NOT NULL,
 author VARCHAR(255) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX referral_events_idx ON referral_events(agency_id, referral_id, created_at DESC);
