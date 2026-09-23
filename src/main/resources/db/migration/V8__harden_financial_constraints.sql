ALTER TABLE fee_items
    ALTER COLUMN bank_account_number TYPE VARCHAR(255);

ALTER TABLE financial_transactions
    ADD CONSTRAINT ck_financial_transactions_evidence
        CHECK (transaction_type = 'INCOME' OR evidence_file_id IS NOT NULL),
    ADD CONSTRAINT ck_financial_transactions_payment_method
        CHECK (transaction_type = 'INCOME' OR payment_method IS NOT NULL);
