package com.dongbang.organization.application.facade;

import com.dongbang.organization.domain.Membership;
import com.dongbang.organization.domain.MembershipStatus;
import com.dongbang.organization.domain.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MembershipAccessFacade {

    private final MembershipRepository membershipRepository;

    public boolean isActiveMember(Long organizationId, Long userId) {
        return membershipRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .map(m -> m.getStatus() == MembershipStatus.ACTIVE)
                .orElse(false);
    }

    public boolean isStaff(Long organizationId, Long userId) {
        return membershipRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .map(m -> m.getStatus() == MembershipStatus.ACTIVE && m.getRole().isStaff())
                .orElse(false);
    }

    public Optional<MembershipSummary> getMembershipSummary(Long organizationId, Long userId) {
        return membershipRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .map(m -> new MembershipSummary(
                        m.getId(),
                        organizationId,
                        m.getUserId(),
                        m.getMemberName(),
                        m.getRole(),
                        m.getStatus()
                ));
    }
}
