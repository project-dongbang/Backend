package com.dongbang.event.infrastructure.persistence;
import com.dongbang.event.domain.EventParticipant;
import com.dongbang.event.domain.repository.EventParticipantRepository;
import org.springframework.data.jpa.repository.JpaRepository;
public interface EventParticipantJpaRepository extends JpaRepository<EventParticipant, Long>, EventParticipantRepository {}
