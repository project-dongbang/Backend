package com.dongbang.organization.application;

import com.dongbang.global.exception.GeneralException;
import com.dongbang.global.response.code.GeneralErrorCode;
import com.dongbang.organization.domain.*;
import com.dongbang.organization.domain.repository.InvitationRepository;
import com.dongbang.organization.domain.repository.MembershipRepository;
import com.dongbang.organization.domain.repository.OrganizationRepository;
import com.dongbang.organization.exception.OrganizationErrorCode;
import com.dongbang.organization.presentation.dto.request.*;
import com.dongbang.organization.presentation.dto.response.CreateOrganizationResponse;
import com.dongbang.organization.presentation.dto.response.InvitationResponse;
import com.dongbang.organization.presentation.dto.response.JoinOrganizationResponse;
import com.dongbang.organization.presentation.dto.response.MemberItemResponse;
import com.dongbang.auth.infrastructure.token.TokenHashService;
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
    private final TokenHashService tokenHashService;

    public CreateOrganizationResponse createOrganization(Long userId, CreateOrganizationRequest request) {
        Organization organization = Organization.builder()
                .name(request.name())
                .slug("org-" + UUID.randomUUID().toString().replace("-", ""))
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

        UpdateOrganizationSettingsRequest settings = request.settings();
        if (settings != null) {
            if (settings.operatingSemester() == null && settings.defaultFeeAmount() == null
                    && settings.paymentAccount() == null) {
                throw new GeneralException(GeneralErrorCode.VALIDATION_ERROR);
            }
        }

        Organization organization = findActiveOrganization(organizationId);
        organization.updateInfo(request.name(), request.description(), request.logoUrl());
        if (settings != null) {
            var account = settings.paymentAccount();
            organization.updateSettings(settings.operatingSemester(), settings.defaultFeeAmount(),
                    account != null ? account.bankName().trim() : null,
                    account != null ? account.accountNumber().trim() : null,
                    account != null ? account.accountHolder().trim() : null);
        }
    }

    public MemberItemResponse updateMemberInfo(Long userId, Long organizationId, Long targetMemberId,
                                               UpdateMemberInfoRequest request) {
        Membership editor = validateStaff(organizationId, userId);
        findActiveOrganization(organizationId);
        if (request.status() == null && request.generation() == null && request.position() == null) {
            throw new GeneralException(GeneralErrorCode.VALIDATION_ERROR);
        }
        Membership target = membershipRepository.findById(targetMemberId)
                .filter(member -> member.getOrganization().getId().equals(organizationId))
                .orElseThrow(() -> new GeneralException(OrganizationErrorCode.MEMBER_NOT_FOUND));
        if (target.getStatus() == MembershipStatus.LEFT || target.getStatus() == MembershipStatus.EXPELLED) {
            throw new GeneralException(OrganizationErrorCode.MEMBER_STATUS_NOT_EDITABLE);
        }
        if (target.getRole().isOwner()) {
            if (!editor.getRole().isOwner()) {
                throw new GeneralException(OrganizationErrorCode.OWNER_REQUIRED);
            }
            if (request.status() == UpdateMemberInfoRequest.ActivityStatus.INACTIVE) {
                throw new GeneralException(OrganizationErrorCode.OWNER_CANNOT_DEACTIVATE);
            }
        }

        if (request.status() != null) {
            target.updateActivityStatus(request.status().toMembershipStatus());
        }
        if (request.generation() != null) {
            String generation = request.generation().trim();
            if (generation.isEmpty()) {
                throw new GeneralException(GeneralErrorCode.VALIDATION_ERROR);
            }
            target.updateGeneration(generation);
        }
        if (request.position() != null) {
            String position = request.position().trim();
            target.updatePosition(position.isEmpty() ? null : position);
        }
        return new MemberItemResponse(target.getId(), target.getUserId(), target.getMemberName(),
                target.getStudentNumber(), target.getGeneration(), target.getPosition(), target.getRole(),
                target.getStatus(), target.getJoinedAt());
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
                .tokenHash(tokenHashService.hash(token))
                .expiresAt(expiresAt)
                .build();
        invitationRepository.save(invitation);

        return new InvitationResponse(token, expiresAt);
    }

    public JoinOrganizationResponse acceptInvitation(Long userId, String token) {
        Invitation invitation = invitationRepository.findByTokenHash(tokenHashService.hash(token))
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
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
                .orElseThrow(() -> new GeneralException(OrganizationErrorCode.MEMBER_NOT_FOUND));

        if (request.role() == MembershipRole.OWNER) {
            throw new GeneralException(OrganizationErrorCode.INVALID_DELEGATION_TARGET,
                    "대표 권한은 대표 위임으로만 변경할 수 있습니다.");
        }

        target.updateRole(request.role());
    }

    public void expelMember(Long userId, Long organizationId, Long targetMemberId) {
        validateOwner(organizationId, userId);

        Membership target = membershipRepository.findById(targetMemberId)
                .filter(m -> m.getOrganization().getId().equals(organizationId))
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
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
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
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
