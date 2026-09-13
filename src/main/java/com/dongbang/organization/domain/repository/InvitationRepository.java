package com.dongbang.organization.domain.repository;

import com.dongbang.organization.domain.Invitation;

import java.util.Optional;

public interface InvitationRepository {
    Invitation save(Invitation invitation);
    Optional<Invitation> findByTokenHash(String tokenHash);
}
