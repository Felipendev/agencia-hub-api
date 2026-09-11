ALTER TABLE sales ADD COLUMN approval_managed BOOLEAN NOT NULL DEFAULT FALSE;
-- Historical manual links may contain duplicates. Preserve them; new approval flows are unique.
CREATE UNIQUE INDEX uq_sales_approval_quotation ON sales(quotation_id)
    WHERE approval_managed = TRUE AND quotation_id IS NOT NULL;
