-- V2__create_photo_and_uploaded_file.sql

CREATE TABLE uploaded_files (
    uploaded_file_id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    uploaded_by_membership_id BIGINT NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    checksum VARCHAR(128) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ NULL,
    CONSTRAINT fk_uploaded_files_organization FOREIGN KEY (organization_id) REFERENCES organizations (organization_id),
    CONSTRAINT fk_uploaded_files_membership FOREIGN KEY (uploaded_by_membership_id) REFERENCES memberships (membership_id)
);

CREATE TABLE photos (
    photo_id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    uploaded_file_id BIGINT NOT NULL,
    membership_id BIGINT NOT NULL,
    title VARCHAR(200) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ NULL,
    CONSTRAINT fk_photos_organization FOREIGN KEY (organization_id) REFERENCES organizations (organization_id),
    CONSTRAINT fk_photos_uploaded_file FOREIGN KEY (uploaded_file_id) REFERENCES uploaded_files (uploaded_file_id),
    CONSTRAINT fk_photos_membership FOREIGN KEY (membership_id) REFERENCES memberships (membership_id)
);

-- 인덱스 생성
CREATE INDEX idx_uploaded_files_org_id ON uploaded_files (organization_id);
CREATE INDEX idx_photos_org_id ON photos (organization_id);
CREATE INDEX idx_photos_cursor ON photos (organization_id, photo_id DESC) WHERE deleted_at IS NULL;