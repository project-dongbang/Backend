package com.dongbang.organization.application.facade;

import com.dongbang.organization.domain.Membership;
import com.dongbang.organization.domain.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Transactional
public class MembershipLinkFacade {

    private final MembershipRepository membershipRepository;

    public int linkExistingMemberships(Long userId, String name, String studentNumber, Instant linkedAt) {
        var memberships = membershipRepository.findAllUnlinkedByIdentity(name, studentNumber);
        memberships.forEach(membership -> membership.linkUser(userId, linkedAt));
        return memberships.size();
    }
}
