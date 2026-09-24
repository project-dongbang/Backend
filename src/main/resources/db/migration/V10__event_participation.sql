ALTER TABLE events
 ADD COLUMN participant_version BIGINT NOT NULL DEFAULT 0,
 ADD COLUMN registration_closed_at TIMESTAMPTZ;
CREATE TABLE event_participants (
 id BIGSERIAL PRIMARY KEY,
 event_id BIGINT NOT NULL REFERENCES events(event_id),
 membership_id BIGINT NOT NULL REFERENCES memberships(membership_id),
 registered_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uq_event_participant UNIQUE(event_id, membership_id)
);
