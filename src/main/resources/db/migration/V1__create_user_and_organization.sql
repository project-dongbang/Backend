-- V1__create_user_and_organization.sql

CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NULL,
    name VARCHAR(100) NULL,
    student_number VARCHAR(20) NULL,
    department VARCHAR(100) NULL,
    profile_image_url VARCHAR(500) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_ONBOARDING',
    onboarding_completed_at TIMESTAMPTZ NULL,
    last_login_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ NULL
);

CREATE TABLE organizations (
    organization_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    description VARCHAR(1000) NULL,
    logo_url VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ NULL,
    CONSTRAINT uq_organizations_slug UNIQUE (slug)
);

CREATE TABLE memberships (
    membership_id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    user_id BIGINT NULL,
    member_name VARCHAR(100) NOT NULL,
    student_number VARCHAR(20) NOT NULL,
    generation VARCHAR(20) NULL,
    position VARCHAR(50) NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    linked_at TIMESTAMPTZ NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMPTZ NULL,
    CONSTRAINT fk_memberships_organization FOREIGN KEY (organization_id) REFERENCES organizations (organization_id),
    CONSTRAINT fk_memberships_user FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE TABLE invitations (
    invitation_id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_invitations_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_invitations_organization FOREIGN KEY (organization_id) REFERENCES organizations (organization_id)
);

-- 인덱스 생성
CREATE INDEX idx_organizations_status ON organizations (status);
CREATE INDEX idx_memberships_organization_id ON memberships (organization_id);
CREATE INDEX idx_memberships_user_id ON memberships (user_id);
CREATE INDEX idx_memberships_org_user ON memberships (organization_id, user_id);
CREATE INDEX idx_invitations_organization_id ON invitations (organization_id);
