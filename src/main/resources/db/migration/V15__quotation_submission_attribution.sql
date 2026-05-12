-- Quem indicou o link público (vendedor/dono) e rastreio da cotação criada a partir do formulário.

ALTER TABLE solicitacao_submissions
    ADD COLUMN referral_seller_id UUID REFERENCES users (id);

ALTER TABLE quotations
    ADD COLUMN creation_source VARCHAR(32) NOT NULL DEFAULT 'INTERNAL',
    ADD COLUMN created_by_user_id UUID REFERENCES users (id),
    ADD COLUMN public_submission_id UUID REFERENCES solicitacao_submissions (id);

CREATE INDEX idx_quotations_public_submission ON quotations (public_submission_id);
CREATE INDEX idx_quotations_created_by ON quotations (created_by_user_id);
