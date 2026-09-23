CREATE TABLE events (
    event_id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    created_by_membership_id BIGINT NOT NULL,
    event_type VARCHAR(20) NOT NULL DEFAULT 'EVENT',
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    location VARCHAR(255) NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    capacity INTEGER NULL,
    registration_opens_at TIMESTAMPTZ NULL,
    registration_closes_at TIMESTAMPTZ NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    canceled_at TIMESTAMPTZ NULL,
    deleted_at TIMESTAMPTZ NULL,
    CONSTRAINT fk_events_organization FOREIGN KEY (organization_id)
        REFERENCES organizations (organization_id),
    CONSTRAINT fk_events_creator FOREIGN KEY (created_by_membership_id)
        REFERENCES memberships (membership_id),
    CONSTRAINT ck_events_type CHECK (event_type IN ('SCHEDULE', 'EVENT')),
    CONSTRAINT ck_events_status CHECK (status IN ('SCHEDULED', 'CANCELED')),
    CONSTRAINT ck_events_period CHECK (ends_at > starts_at),
    CONSTRAINT ck_events_capacity CHECK (capacity IS NULL OR capacity > 0),
    CONSTRAINT ck_events_registration_end CHECK (
        registration_closes_at IS NULL OR registration_closes_at <= starts_at),
    CONSTRAINT ck_events_registration_period CHECK (
        registration_opens_at IS NULL OR registration_closes_at IS NULL
        OR registration_opens_at <= registration_closes_at),
    CONSTRAINT ck_events_schedule CHECK (
        event_type = 'EVENT' OR
        (capacity IS NULL AND registration_opens_at IS NULL AND registration_closes_at IS NULL))
);

CREATE INDEX idx_events_calendar ON events (organization_id, starts_at, event_id)
    WHERE deleted_at IS NULL;

-- TODO(event): participant_version / 조기 마감 이력은 참가자 관리 구현 시 추가한다.
-- TODO(attendance): 세션은 행사당 UNIQUE(event_id), TTL 600초, QR 1회 생성 정책으로 별도 추가한다.
