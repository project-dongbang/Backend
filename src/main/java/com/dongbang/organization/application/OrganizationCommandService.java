package com.dongbang.organization.application;

import com.dongbang.global.exception.GeneralException;
import com.dongbang.organization.domain.*;
import com.dongbang.organization.domain.repository.InvitationRepository;
import com.dongbang.organization.domain.repository.MembershipRepository;
import com.dongbang.organization.domain.repository.OrganizationRepository;
import com.dongbang.organization.exception.OrganizationErrorCode;
import com.dongbang.organization.presentation.dto.request.*;
import com.dongbang.organization.presentation.dto.response.CreateOrganizationResponse;
import com.dongbang.organization.presentation.dto.response.InvitationResponse;
import com.dongbang.organization.presentation.dto.response.JoinOrganizationResponse;
import com.dongbang.user.application.facade.UserAccountFacade;
import com.dongbang.user.application.facade.UserAccountSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationCommandService {

    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final InvitationRepository invitationRepository;
    private final UserAccountFacade userAccountFacade;

    public CreateOrganizationResponse createOrganization(Long userId, CreateOrganizationRequest request) {
        if (organizationRepository.existsBySlug(request.slug())) {
            throw new GeneralException(OrganizationErrorCode.SLUG_ALREADY_EXISTS);
        }

        Organization organization = Organization.builder()
                .name(request.name())
                .slug(request.slug())
                .description(request.description())
                .logoUrl(request.logoUrl())
                .build();
        Organization saved = organizationRepository.save(organization);

        UserAccountSummary account = userAccountFacade.getAccount(userId);

        Membership owner = Membership.builder()
                .organization(saved)
                .userId(userId)
                .memberName(account.name())
                .studentNumber(account.studentNumber())
                .role(MembershipRole.OWNER)
                .build();
        membershipRepository.save(owner);

        return new CreateOrganizationResponse(saved.getId(), saved.getSlug());
    }

    public void updateOrganization(Long userId, Long organizationId, UpdateOrganizationRequest request) {
        validateStaff(organizationId, userId);

        Organization organization = findActiveOrganization(organizationId);
        organization.updateInfo(request.name(), request.description(), request.logoUrl());
    }

    public void deleteOrganization(Long userId, Long organizationId) {
        validateOwner(organizationId, userId);

        Organization organization = findActiveOrganization(organizationId);
        organization.delete();
    }

    public InvitationResponse createInvitation(Long userId, Long organizationId, CreateInvitationRequest request) {
        validateStaff(organizationId, userId);
        Organization organization = findActiveOrganization(organizationId);

        String token = UUID.randomUUID().toString().replace("-", "");
        int hours = request != null ? request.getEffectiveExpiresInHours() : 24;
        Instant expiresAt = Instant.now().plus(hours, ChronoUnit.HOURS);

        Invitation invitation = Invitation.builder()
                .organization(organization)
                .tokenHash(token)
                .expiresAt(expiresAt)
                .build();
        invitationRepository.save(invitation);

        return new InvitationResponse(token, expiresAt);
    }

    public JoinOrganizationResponse acceptInvitation(Long userId, String token) {
        Invitation invitation = invitationRepository.findByTokenHash(token)
                .orElseThrow(() -> new GeneralException(OrganizationErrorCode.INVALID_INVITATION_TOKEN));

        if (invitation.isExpired()) {
            throw new GeneralException(OrganizationErrorCode.INVALID_INVITATION_TOKEN);
        }

        Organization organization = invitation.getOrganization();
        if (organization.getStatus() != OrganizationStatus.ACTIVE) {
            throw new GeneralException(OrganizationErrorCode.ORGANIZATION_NOT_FOUND);
        }

        membershipRepository.findByOrganizationIdAndUserId(organization.getId(), userId)
                .ifPresent(existing -> {
                    throw new GeneralException(OrganizationErrorCode.ALREADY_JOINED_MEMBER);
                });

        UserAccountSummary account = userAccountFacade.getAccount(userId);

        Membership member = Membership.builder()
                .organization(organization)
                .userId(userId)
                .memberName(account.name())
                .studentNumber(account.studentNumber())
                .role(MembershipRole.MEMBER)
                .build();
        Membership saved = membershipRepository.save(member);

        return new JoinOrganizationResponse(organization.getId(), saved.getId(), saved.getRole());
    }

    public void changeMemberRole(Long userId, Long organizationId, Long targetMemberId, ChangeRoleRequest request) {
        validateOwner(organizationId, userId);

        Membership target = membershipRepository.findById(targetMemberId)
                .filter(m -> m.getOrganization().getId().equals(organizationId))
                .orElseThrow(() -> new GeneralException(OrganizationErrorCode.MEMBER_NOT_FOUND));

        target.updateRole(request.role());
    }

    public void expelMember(Long userId, Long organizationId, Long targetMemberId) {
        validateOwner(organizationId, userId);

        Membership target = membershipRepository.findById(targetMemberId)
                .filter(m -> m.getOrganization().getId().equals(organizationId))
                .orElseThrow(() -> new GeneralException(OrganizationErrorCode.MEMBER_NOT_FOUND));

        if (target.getRole().isOwner()) {
            throw new GeneralException(OrganizationErrorCode.INVALID_DELEGATION_TARGET, "회장은 강퇴할 수 없습니다.");
        }

        target.expel();
    }

    public void delegateOwner(Long userId, Long organizationId, DelegateOwnerRequest request) {
        Membership currentOwner = validateOwner(organizationId, userId);

        Membership target = membershipRepository.findById(request.targetMemberId())
                .filter(m -> m.getOrganization().getId().equals(organizationId))
                .orElseThrow(() -> new GeneralException(OrganizationErrorCode.MEMBER_NOT_FOUND));

        if (target.getRole() != MembershipRole.ADMIN) {
            throw new GeneralException(OrganizationErrorCode.INVALID_DELEGATION_TARGET);
        }

        target.updateRole(MembershipRole.OWNER);
        currentOwner.updateRole(MembershipRole.ADMIN);
    }

    public void leaveOrganization(Long userId, Long organizationId) {
        Membership membership = membershipRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .orElseThrow(() -> new GeneralException(OrganizationErrorCode.MEMBER_NOT_FOUND));

        if (membership.getRole().isOwner()) {
            throw new GeneralException(OrganizationErrorCode.OWNER_CANNOT_LEAVE);
        }

        membership.leave();
    }

    private Organization findActiveOrganization(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .filter(o -> o.getStatus() == OrganizationStatus.ACTIVE)
                .orElseThrow(() -> new GeneralException(OrganizationErrorCode.ORGANIZATION_NOT_FOUND));
    }

    private Membership validateStaff(Long organizationId, Long userId) {
        Membership membership = membershipRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .orElseThrow(() -> new GeneralException(OrganizationErrorCode.STAFF_REQUIRED));
        if (!membership.getRole().isStaff()) {
            throw new GeneralException(OrganizationErrorCode.STAFF_REQUIRED);
        }
        return membership;
    }

    private Membership validateOwner(Long organizationId, Long userId) {
        Membership membership = membershipRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .orElseThrow(() -> new GeneralException(OrganizationErrorCode.OWNER_REQUIRED));
        if (!membership.getRole().isOwner()) {
            throw new GeneralException(OrganizationErrorCode.OWNER_REQUIRED);
        }
        return membership;
    }
}
