CREATE TABLE receipts (
    receipt_id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    uploaded_by_membership_id BIGINT NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    checksum VARCHAR(128),
    ocr_text TEXT NOT NULL,
    store_name VARCHAR(255),
    purchased_at DATE,
    total_amount NUMERIC(15, 2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_receipts_organization FOREIGN KEY (organization_id) REFERENCES organizations (organization_id),
    CONSTRAINT fk_receipts_membership FOREIGN KEY (uploaded_by_membership_id) REFERENCES memberships (membership_id)
);

CREATE INDEX idx_receipts_org_created ON receipts (organization_id, receipt_id DESC);
