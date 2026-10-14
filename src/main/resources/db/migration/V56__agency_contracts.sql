-- F02 — Contratos versionados por agência (TODO-046).
-- Versão provisória: o integrador atribui a numeração final antes do merge.

CREATE TABLE contract_templates (
 id UUID PRIMARY KEY,
 agency_id UUID NOT NULL REFERENCES agencies(id),
 name VARCHAR(200) NOT NULL,
 description VARCHAR(1000),
 current_version INTEGER NOT NULL DEFAULT 1 CHECK (current_version >= 1),
 archived BOOLEAN NOT NULL DEFAULT false,
 row_version INTEGER NOT NULL DEFAULT 1,
 created_by VARCHAR(255) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX contract_templates_agency_idx ON contract_templates(agency_id, archived, lower(name));

-- Cada alteração do conteúdo gera uma nova versão imutável.
CREATE TABLE contract_template_versions (
 id UUID PRIMARY KEY,
 template_id UUID NOT NULL REFERENCES contract_templates(id) ON DELETE CASCADE,
 agency_id UUID NOT NULL REFERENCES agencies(id),
 version INTEGER NOT NULL CHECK (version >= 1),
 body TEXT NOT NULL,
 author VARCHAR(255) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE (template_id, version)
);

CREATE TABLE contracts (
 id UUID PRIMARY KEY,
 agency_id UUID NOT NULL REFERENCES agencies(id),
 customer_id UUID NOT NULL REFERENCES customers(id),
 template_id UUID NOT NULL REFERENCES contract_templates(id),
 template_version INTEGER NOT NULL,
 title VARCHAR(200) NOT NULL,
 plan_name VARCHAR(200),
 price NUMERIC(19,2) CHECK (price IS NULL OR price >= 0),
 start_date DATE,
 end_date DATE,
 payment_mode VARCHAR(16) CHECK (payment_mode IS NULL OR payment_mode IN ('CASH','INSTALLMENTS')),
 installments INTEGER CHECK (installments IS NULL OR installments BETWEEN 1 AND 120),
 payment_terms VARCHAR(2000),
 notes VARCHAR(4000),
 status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','SENT','SIGNED','CANCELLED')),
 data_revision INTEGER NOT NULL DEFAULT 1,
 row_version INTEGER NOT NULL DEFAULT 1,
 sent_on DATE,
 signed_on DATE,
 cancelled_at TIMESTAMPTZ,
 cancel_reason VARCHAR(1000),
 created_by VARCHAR(255) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date),
 FOREIGN KEY (template_id, template_version) REFERENCES contract_template_versions(template_id, version)
);
CREATE INDEX contracts_agency_customer_idx ON contracts(agency_id, customer_id, created_at DESC);
CREATE INDEX contracts_agency_status_idx ON contracts(agency_id, status, updated_at DESC);

-- Documento emitido: retrato imutável do modelo e dos dados usados na geração.
CREATE TABLE contract_documents (
 id UUID PRIMARY KEY,
 contract_id UUID NOT NULL REFERENCES contracts(id),
 agency_id UUID NOT NULL REFERENCES agencies(id),
 version INTEGER NOT NULL CHECK (version >= 1),
 data_revision INTEGER NOT NULL,
 template_id UUID NOT NULL,
 template_version INTEGER NOT NULL,
 template_body TEXT NOT NULL,
 rendered_content TEXT NOT NULL,
 data_snapshot JSONB NOT NULL,
 content_sha256 CHAR(64) NOT NULL,
 author VARCHAR(255) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE (contract_id, version)
);

CREATE TABLE contract_attachments (
 id UUID PRIMARY KEY,
 contract_id UUID NOT NULL REFERENCES contracts(id),
 agency_id UUID NOT NULL REFERENCES agencies(id),
 kind VARCHAR(24) NOT NULL CHECK (kind IN ('SIGNED_EVIDENCE','SUPPORTING')),
 document_version INTEGER,
 description VARCHAR(1000),
 filename VARCHAR(255) NOT NULL,
 content_type VARCHAR(150) NOT NULL,
 byte_size BIGINT NOT NULL CHECK (byte_size > 0),
 content BYTEA NOT NULL,
 author VARCHAR(255) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX contract_attachments_contract_idx ON contract_attachments(contract_id, created_at);

-- Vínculo opcional e explícito a recebíveis já existentes; não cria venda nem receita.
CREATE TABLE contract_receivable_links (
 contract_id UUID NOT NULL REFERENCES contracts(id),
 receivable_id UUID NOT NULL REFERENCES receivables(id) ON DELETE CASCADE,
 agency_id UUID NOT NULL REFERENCES agencies(id),
 author VARCHAR(255) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 PRIMARY KEY (contract_id, receivable_id)
);

-- Auditoria própria do domínio; sem FK para preservar o registro de exclusões.
CREATE TABLE contract_audit (
 id UUID PRIMARY KEY,
 agency_id UUID NOT NULL REFERENCES agencies(id),
 resource_type VARCHAR(16) NOT NULL CHECK (resource_type IN ('TEMPLATE','CONTRACT')),
 resource_id UUID NOT NULL,
 customer_id UUID,
 action VARCHAR(40) NOT NULL,
 detail JSONB NOT NULL,
 author VARCHAR(255) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX contract_audit_resource_idx ON contract_audit(agency_id, resource_id, created_at DESC);
CREATE INDEX contract_audit_customer_idx ON contract_audit(agency_id, customer_id, created_at DESC);
