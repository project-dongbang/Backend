CREATE TABLE notifications (
    notification_id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    reference_type VARCHAR(50) NULL,
    reference_id BIGINT NULL,
    read_at TIMESTAMPTZ NULL,
    deduplication_key VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_organization FOREIGN KEY (organization_id) REFERENCES organizations (organization_id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT uq_notifications_deduplication_key UNIQUE (deduplication_key)
);

CREATE INDEX idx_notifications_user_org_created
    ON notifications (user_id, organization_id, created_at DESC, notification_id DESC);

CREATE INDEX idx_notifications_user_org_unread
    ON notifications (user_id, organization_id, notification_id DESC)
    WHERE read_at IS NULL;
