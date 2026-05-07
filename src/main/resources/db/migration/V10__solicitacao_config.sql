-- V10__solicitacao_config.sql
-- Stores public quotation request form configuration per agency.

CREATE TABLE solicitacao_configs (
    id              UUID         NOT NULL PRIMARY KEY,
    agency_id       UUID         NOT NULL REFERENCES agencies(id),
    slug            VARCHAR(128) NOT NULL,
    titulo_pagina   VARCHAR(512) NOT NULL,
    texto_intro     TEXT         NOT NULL DEFAULT '',
    logo_data_url   TEXT,
    nome_marca      VARCHAR(255) NOT NULL DEFAULT 'Agência',
    links_sociais   JSONB        NOT NULL DEFAULT '[]',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(agency_id, slug)
);

CREATE INDEX idx_solicitacao_configs_agency_slug ON solicitacao_configs(agency_id, slug);
