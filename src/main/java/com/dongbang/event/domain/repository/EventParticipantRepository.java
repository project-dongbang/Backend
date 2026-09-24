package com.dongbang.event.domain.repository;
import com.dongbang.event.domain.EventParticipant;
import java.util.List;
import java.util.Optional;
public interface EventParticipantRepository {
    EventParticipant save(EventParticipant participant);
    void delete(EventParticipant participant);
    int countByEventId(Long eventId);
    Optional<EventParticipant> findByEventIdAndMembershipId(Long eventId, Long membershipId);
    Optional<EventParticipant> findRegistration(Long eventId, Long membershipId);
    List<EventParticipant> findByEventIdOrderByRegisteredAtAscIdAsc(Long eventId);
    List<EventParticipant> findByMembershipId(Long membershipId);
}
