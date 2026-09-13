package com.dongbang.organization.infrastructure.persistence;

import com.dongbang.organization.domain.Invitation;
import com.dongbang.organization.domain.repository.InvitationRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvitationJpaRepository extends JpaRepository<Invitation, Long>, InvitationRepository {
    @Override
    Optional<Invitation> findByTokenHash(String tokenHash);
}
