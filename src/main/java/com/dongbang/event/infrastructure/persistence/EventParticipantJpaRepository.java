package com.dongbang.event.infrastructure.persistence;
import com.dongbang.event.domain.EventParticipant;
import com.dongbang.event.domain.repository.EventParticipantRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
public interface EventParticipantJpaRepository extends JpaRepository<EventParticipant, Long>, EventParticipantRepository {
    @Query("select count(p) from EventParticipant p where p.eventId = :eventId and p.status = 'REGISTERED'")
    int countByEventId(Long eventId);

    @Query("select p from EventParticipant p where p.eventId = :eventId and p.membershipId = :membershipId and p.status = 'REGISTERED'")
    Optional<EventParticipant> findByEventIdAndMembershipId(Long eventId, Long membershipId);

    @Query("select p from EventParticipant p where p.eventId = :eventId and p.membershipId = :membershipId")
    Optional<EventParticipant> findRegistration(Long eventId, Long membershipId);

    @Query("select p from EventParticipant p where p.eventId = :eventId and p.status = 'REGISTERED' order by p.registeredAt, p.id")
    List<EventParticipant> findByEventIdOrderByRegisteredAtAscIdAsc(Long eventId);
}
