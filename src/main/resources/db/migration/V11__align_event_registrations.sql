ALTER TABLE event_participants RENAME TO event_registrations;
ALTER TABLE event_registrations RENAME COLUMN id TO event_registration_id;
ALTER TABLE event_registrations
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'REGISTERED',
    ADD COLUMN canceled_at TIMESTAMPTZ,
    ADD CONSTRAINT ck_event_registration_status CHECK (status IN ('REGISTERED', 'CANCELED'));

CREATE INDEX idx_event_registrations_active ON event_registrations (event_id)
    WHERE status = 'REGISTERED';
