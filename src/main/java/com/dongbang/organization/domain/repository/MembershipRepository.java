package com.dongbang.organization.domain.repository;

import com.dongbang.organization.domain.Membership;
import com.dongbang.organization.domain.MembershipStatus;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository {
    Membership save(Membership membership);
    Optional<Membership> findById(Long id);
    Optional<Membership> findByOrganizationIdAndUserId(Long organizationId, Long userId);
    List<Membership> findAllByUserId(Long userId);
    List<Membership> findAllByOrganizationId(Long organizationId);
    List<Membership> findAllByOrganizationIdAndStatus(Long organizationId, MembershipStatus status);
    List<Membership> findAllByIdIn(java.util.Collection<Long> ids);
    List<Membership> findAllUnlinkedByIdentity(String memberName, String studentNumber);
}
