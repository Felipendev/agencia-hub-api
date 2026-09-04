ALTER TABLE sales ADD COLUMN recurrence_frequency VARCHAR(32);

ALTER TABLE receivables ADD COLUMN payment_method VARCHAR(32);
ALTER TABLE receivables ADD COLUMN bank_account VARCHAR(128);
