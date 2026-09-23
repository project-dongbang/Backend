ALTER TABLE users
    ADD COLUMN profile_image_storage_key VARCHAR(500) NULL;

CREATE TABLE fee_items (
    fee_item_id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    due_date DATE NOT NULL,
    description VARCHAR(1000) NULL,
    bank_name VARCHAR(50) NOT NULL,
    bank_account_number VARCHAR(512) NOT NULL,
    account_holder VARCHAR(100) NOT NULL,
    created_by_membership_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fee_items_organization FOREIGN KEY (organization_id) REFERENCES organizations (organization_id),
    CONSTRAINT fk_fee_items_creator FOREIGN KEY (created_by_membership_id) REFERENCES memberships (membership_id)
);

CREATE TABLE fee_categories (
    fee_category_id BIGSERIAL PRIMARY KEY,
    fee_item_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    amount NUMERIC(14, 2) NOT NULL CHECK (amount > 0),
    display_order INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fee_categories_item FOREIGN KEY (fee_item_id) REFERENCES fee_items (fee_item_id) ON DELETE CASCADE,
    CONSTRAINT uq_fee_categories_item_name UNIQUE (fee_item_id, name)
);

CREATE TABLE fee_targets (
    fee_target_id BIGSERIAL PRIMARY KEY,
    fee_item_id BIGINT NOT NULL,
    fee_category_id BIGINT NOT NULL,
    membership_id BIGINT NOT NULL,
    amount_due NUMERIC(14, 2) NOT NULL CHECK (amount_due > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    status_memo VARCHAR(200) NULL,
    paid_at TIMESTAMPTZ NULL,
    status_changed_at TIMESTAMPTZ NULL,
    status_changed_by_membership_id BIGINT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fee_targets_item FOREIGN KEY (fee_item_id) REFERENCES fee_items (fee_item_id) ON DELETE CASCADE,
    CONSTRAINT fk_fee_targets_category FOREIGN KEY (fee_category_id) REFERENCES fee_categories (fee_category_id) ON DELETE CASCADE,
    CONSTRAINT fk_fee_targets_membership FOREIGN KEY (membership_id) REFERENCES memberships (membership_id),
    CONSTRAINT fk_fee_targets_changer FOREIGN KEY (status_changed_by_membership_id) REFERENCES memberships (membership_id),
    CONSTRAINT uq_fee_targets_item_membership UNIQUE (fee_item_id, membership_id)
);

CREATE TABLE financial_transactions (
    transaction_id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    fee_item_id BIGINT NULL,
    fee_target_id BIGINT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL,
    amount NUMERIC(14, 2) NOT NULL CHECK (amount > 0),
    occurred_on DATE NOT NULL,
    vendor VARCHAR(255) NOT NULL,
    payment_method VARCHAR(50) NULL,
    memo VARCHAR(1000) NULL,
    evidence_file_id BIGINT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'POSTED',
    created_by_membership_id BIGINT NOT NULL,
    void_reason VARCHAR(255) NULL,
    voided_by_membership_id BIGINT NULL,
    voided_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_financial_transactions_organization FOREIGN KEY (organization_id) REFERENCES organizations (organization_id),
    CONSTRAINT fk_financial_transactions_item FOREIGN KEY (fee_item_id) REFERENCES fee_items (fee_item_id) ON DELETE SET NULL,
    CONSTRAINT fk_financial_transactions_target FOREIGN KEY (fee_target_id) REFERENCES fee_targets (fee_target_id) ON DELETE SET NULL,
    CONSTRAINT fk_financial_transactions_evidence FOREIGN KEY (evidence_file_id) REFERENCES uploaded_files (uploaded_file_id),
    CONSTRAINT fk_financial_transactions_creator FOREIGN KEY (created_by_membership_id) REFERENCES memberships (membership_id),
    CONSTRAINT fk_financial_transactions_voider FOREIGN KEY (voided_by_membership_id) REFERENCES memberships (membership_id)
);

CREATE TABLE audit_logs (
    audit_log_id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    actor_membership_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    before_snapshot TEXT NULL,
    after_snapshot TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_logs_organization FOREIGN KEY (organization_id) REFERENCES organizations (organization_id),
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_membership_id) REFERENCES memberships (membership_id)
);

CREATE INDEX idx_fee_items_org_created ON fee_items (organization_id, fee_item_id DESC);
CREATE INDEX idx_fee_targets_item_status ON fee_targets (fee_item_id, status, fee_target_id);
CREATE INDEX idx_fee_targets_membership ON fee_targets (membership_id, fee_target_id DESC);
CREATE INDEX idx_financial_transactions_org_date ON financial_transactions (organization_id, occurred_on DESC, transaction_id DESC);
CREATE UNIQUE INDEX uq_financial_transactions_active_fee_target
    ON financial_transactions (fee_target_id)
    WHERE fee_target_id IS NOT NULL AND transaction_type = 'INCOME' AND status = 'POSTED';
CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id, created_at DESC);
