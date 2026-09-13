package com.dongbang.organization.application;

import com.dongbang.global.exception.GeneralException;
import com.dongbang.organization.domain.Membership;
import com.dongbang.organization.domain.MembershipStatus;
import com.dongbang.organization.domain.Organization;
import com.dongbang.organization.domain.OrganizationStatus;
import com.dongbang.organization.domain.repository.MembershipRepository;
import com.dongbang.organization.domain.repository.OrganizationRepository;
import com.dongbang.organization.exception.OrganizationErrorCode;
import com.dongbang.organization.presentation.dto.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationQueryService {

    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;

    public MyOrganizationResponse getMyOrganizations(Long userId) {
        List<Membership> memberships = membershipRepository.findAllByUserId(userId);
        List<MyOrganizationItemResponse> items = memberships.stream()
                .filter(m -> m.getOrganization().getStatus() == OrganizationStatus.ACTIVE)
                .map(m -> new MyOrganizationItemResponse(
                        m.getOrganization().getId(),
                        m.getOrganization().getName(),
                        m.getOrganization().getSlug(),
                        m.getOrganization().getLogoUrl(),
                        m.getRole(),
                        m.getJoinedAt()
                ))
                .toList();

        return new MyOrganizationResponse(items);
    }

    public OrganizationDetailResponse getOrganizationDetail(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .filter(o -> o.getStatus() == OrganizationStatus.ACTIVE)
                .orElseThrow(() -> new GeneralException(OrganizationErrorCode.ORGANIZATION_NOT_FOUND));

        long memberCount = membershipRepository.findAllByOrganizationIdAndStatus(organizationId, MembershipStatus.ACTIVE).size();

        return new OrganizationDetailResponse(
                organization.getId(),
                organization.getName(),
                organization.getSlug(),
                organization.getDescription(),
                organization.getLogoUrl(),
                memberCount,
                organization.getCreatedAt()
        );
    }

    public MemberListResponse getMembers(Long userId, Long organizationId, MembershipStatus statusFilter) {
        Membership caller = membershipRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .orElseThrow(() -> new GeneralException(OrganizationErrorCode.STAFF_REQUIRED));

        if (!caller.getRole().isStaff()) {
            throw new GeneralException(OrganizationErrorCode.STAFF_REQUIRED);
        }

        List<Membership> memberships = (statusFilter != null)
                ? membershipRepository.findAllByOrganizationIdAndStatus(organizationId, statusFilter)
                : membershipRepository.findAllByOrganizationId(organizationId);

        List<MemberItemResponse> items = memberships.stream()
                .map(m -> new MemberItemResponse(
                        m.getId(),
                        m.getUserId(),
                        m.getMemberName(),
                        m.getStudentNumber(),
                        m.getGeneration(),
                        m.getPosition(),
                        m.getRole(),
                        m.getStatus(),
                        m.getJoinedAt()
                ))
                .toList();

        return new MemberListResponse(items);
    }
}
