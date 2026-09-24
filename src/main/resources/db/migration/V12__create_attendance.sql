CREATE TABLE attendance_sessions (
    attendance_session_id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    opened_by_membership_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    qr_token VARCHAR(100) NOT NULL,
    qr_version INTEGER NOT NULL DEFAULT 1,
    qr_ttl_seconds INTEGER NOT NULL DEFAULT 600,
    opened_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_attendance_sessions_event UNIQUE (event_id),
    CONSTRAINT uq_attendance_sessions_qr_token UNIQUE (qr_token),
    CONSTRAINT ck_attendance_sessions_status CHECK (status IN ('OPEN', 'CLOSED')),
    CONSTRAINT ck_attendance_sessions_ttl CHECK (qr_ttl_seconds = 600),
    CONSTRAINT fk_attendance_sessions_event FOREIGN KEY (event_id) REFERENCES events (event_id),
    CONSTRAINT fk_attendance_sessions_opener FOREIGN KEY (opened_by_membership_id) REFERENCES memberships (membership_id)
);

CREATE TABLE attendance_records (
    attendance_record_id BIGSERIAL PRIMARY KEY,
    attendance_session_id BIGINT NOT NULL,
    membership_id BIGINT NOT NULL,
    recorded_by_membership_id BIGINT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ABSENT',
    source VARCHAR(20) NULL,
    reason VARCHAR(500) NULL,
    checked_at TIMESTAMPTZ NULL,
    target_active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_attendance_records_session_member UNIQUE (attendance_session_id, membership_id),
    CONSTRAINT ck_attendance_records_status CHECK (status IN ('PRESENT', 'ABSENT')),
    CONSTRAINT ck_attendance_records_source CHECK (source IS NULL OR source IN ('QR', 'MANUAL')),
    CONSTRAINT fk_attendance_records_session FOREIGN KEY (attendance_session_id) REFERENCES attendance_sessions (attendance_session_id),
    CONSTRAINT fk_attendance_records_member FOREIGN KEY (membership_id) REFERENCES memberships (membership_id),
    CONSTRAINT fk_attendance_records_recorder FOREIGN KEY (recorded_by_membership_id) REFERENCES memberships (membership_id)
);

ALTER TABLE audit_logs ADD COLUMN reason VARCHAR(500) NULL;

CREATE INDEX idx_attendance_records_session_status
    ON attendance_records (attendance_session_id, status, attendance_record_id);
CREATE INDEX idx_attendance_records_membership
    ON attendance_records (membership_id, attendance_record_id DESC);
