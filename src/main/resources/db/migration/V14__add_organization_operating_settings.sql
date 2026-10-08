ALTER TABLE organizations
    ADD COLUMN operating_semester VARCHAR(6) NULL,
    ADD COLUMN default_fee_amount NUMERIC(12, 0) NULL,
    ADD COLUMN fee_bank_name VARCHAR(50) NULL,
    ADD COLUMN fee_account_number VARCHAR(50) NULL,
    ADD COLUMN fee_account_holder VARCHAR(100) NULL;

ALTER TABLE organizations
    ADD CONSTRAINT ck_organizations_default_fee_amount_nonnegative
        CHECK (default_fee_amount IS NULL OR default_fee_amount >= 0),
    ADD CONSTRAINT ck_organizations_fee_account_complete
        CHECK (
            (fee_bank_name IS NULL AND fee_account_number IS NULL AND fee_account_holder IS NULL)
            OR (fee_bank_name IS NOT NULL AND fee_account_number IS NOT NULL AND fee_account_holder IS NOT NULL)
        );
