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

    public Optional<MembershipSummary> getMembershipSummaryById(Long membershipId) {
        return membershipRepository.findById(membershipId)
                .map(m -> new MembershipSummary(
                        m.getId(),
                        m.getOrganization().getId(),
                        m.getUserId(),
                        m.getMemberName(),
                        m.getRole(),
                        m.getStatus()
                ));
    }

    public java.util.Map<Long, MembershipSummary> getMembershipSummariesByIds(java.util.Collection<Long> membershipIds) {
        if (membershipIds == null || membershipIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        return membershipRepository.findAllByIdIn(membershipIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        Membership::getId,
                        m -> new MembershipSummary(
                                m.getId(),
                                m.getOrganization().getId(),
                                m.getUserId(),
                                m.getMemberName(),
                                m.getRole(),
                                m.getStatus()
                        )
                ));
    }

    public long getActiveMemberCount(Long organizationId) {
        return membershipRepository.findAllByOrganizationIdAndStatus(organizationId, MembershipStatus.ACTIVE).size();
    }
}
