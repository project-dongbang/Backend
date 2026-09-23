package com.dongbang.event.domain;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
@Entity
@Table(name = "event_participants", uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "membership_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventParticipant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long eventId;
    @Column(nullable = false) private Long membershipId;
    @Column(nullable = false) private Instant registeredAt;
    public EventParticipant(Long eventId, Long membershipId, Instant registeredAt) {
        this.eventId = eventId; this.membershipId = membershipId; this.registeredAt = registeredAt;
    }
}
