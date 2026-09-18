-- A submissão é um registro de caixa de entrada. Ela não deve voltar a ficar
-- pendente quando a cotação à qual foi convertida for removida.
ALTER TABLE solicitacao_submissions
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN status_updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    ADD COLUMN converted_at TIMESTAMP WITH TIME ZONE;

-- Reconcilia o legado: uma submissão já vinculada a uma cotação foi convertida,
-- mesmo se essa cotação vier a ser removida posteriormente.
UPDATE solicitacao_submissions s
SET status = 'CONVERTED',
    converted_at = COALESCE(s.created_at, NOW()),
    status_updated_at = NOW()
WHERE EXISTS (
    SELECT 1 FROM quotations q WHERE q.public_submission_id = s.id
);

CREATE INDEX idx_solicitacao_submissions_agency_status_created
    ON solicitacao_submissions (agency_id, status, created_at DESC);
