package com.dongbang.organization.presentation;

import com.dongbang.global.response.ApiResponse;
import com.dongbang.global.response.code.GeneralSuccessCode;
import com.dongbang.global.security.CurrentUserId;
import com.dongbang.organization.application.OrganizationCommandService;
import com.dongbang.organization.application.OrganizationQueryService;
import com.dongbang.organization.domain.MembershipStatus;
import com.dongbang.organization.presentation.dto.request.*;
import com.dongbang.organization.presentation.dto.response.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationCommandService commandService;
    private final OrganizationQueryService queryService;

    // 1. 동아리 생성
    @PostMapping("/api/v1/organizations")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreateOrganizationResponse> createOrganization(
            @CurrentUserId Long userId,
            @Valid @RequestBody CreateOrganizationRequest request
    ) {
        CreateOrganizationResponse response = commandService.createOrganization(userId, request);
        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, response);
    }

    // 2. 동아리 수정
    @PatchMapping("/api/v1/organizations/{organizationId}")
    public ApiResponse<Void> updateOrganization(
            @CurrentUserId Long userId,
            @PathVariable Long organizationId,
            @Valid @RequestBody UpdateOrganizationRequest request
    ) {
        commandService.updateOrganization(userId, organizationId, request);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }

    // 3. 동아리 삭제
    @DeleteMapping("/api/v1/organizations/{organizationId}")
    public ApiResponse<Void> deleteOrganization(
            @CurrentUserId Long userId,
            @PathVariable Long organizationId
    ) {
        commandService.deleteOrganization(userId, organizationId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }

    // 4. 동아리 초대링크 생성
    @PostMapping("/api/v1/organizations/{organizationId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InvitationResponse> createInvitation(
            @CurrentUserId Long userId,
            @PathVariable Long organizationId,
            @Valid @RequestBody(required = false) CreateInvitationRequest request
    ) {
        InvitationResponse response = commandService.createInvitation(userId, organizationId, request);
        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, response);
    }

    // 5. 동아리 회원 목록 조회
    @GetMapping("/api/v1/organizations/{organizationId}/members")
    public ApiResponse<MemberListResponse> getMembers(
            @CurrentUserId Long userId,
            @PathVariable Long organizationId,
            @RequestParam(required = false) MembershipStatus status
    ) {
        MemberListResponse response = queryService.getMembers(userId, organizationId, status);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, response);
    }

    // 6. 동아리 회원 권한 변경
    @PatchMapping("/api/v1/organizations/{organizationId}/members/{memberId}/role")
    public ApiResponse<Void> changeMemberRole(
            @CurrentUserId Long userId,
            @PathVariable Long organizationId,
            @PathVariable Long memberId,
            @Valid @RequestBody ChangeRoleRequest request
    ) {
        commandService.changeMemberRole(userId, organizationId, memberId, request);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }

    // 7. 동아리 강퇴
    @DeleteMapping("/api/v1/organizations/{organizationId}/members/{memberId}")
    public ApiResponse<Void> expelMember(
            @CurrentUserId Long userId,
            @PathVariable Long organizationId,
            @PathVariable Long memberId
    ) {
        commandService.expelMember(userId, organizationId, memberId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }

    // 8. 대표 권한 위임
    @PatchMapping("/api/v1/organizations/{organizationId}/owner")
    public ApiResponse<Void> delegateOwner(
            @CurrentUserId Long userId,
            @PathVariable Long organizationId,
            @Valid @RequestBody DelegateOwnerRequest request
    ) {
        commandService.delegateOwner(userId, organizationId, request);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }

    // 9. 동아리 참여(코드)
    @PostMapping("/api/v1/invitations/{token}/accept")
    public ApiResponse<JoinOrganizationResponse> acceptInvitation(
            @CurrentUserId Long userId,
            @PathVariable String token
    ) {
        JoinOrganizationResponse response = commandService.acceptInvitation(userId, token);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, response);
    }

    // 10. 동아리 탈퇴
    @DeleteMapping("/api/v1/organizations/{organizationId}/members/me")
    public ApiResponse<Void> leaveOrganization(
            @CurrentUserId Long userId,
            @PathVariable Long organizationId
    ) {
        commandService.leaveOrganization(userId, organizationId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }

    // 11. 내 참여 동아리 목록 조회
    @GetMapping("/api/v1/users/me/organizations")
    public ApiResponse<MyOrganizationResponse> getMyOrganizations(
            @CurrentUserId Long userId
    ) {
        MyOrganizationResponse response = queryService.getMyOrganizations(userId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, response);
    }

    // 12. 동아리 상세조회
    @GetMapping("/api/v1/organizations/{organizationId}")
    public ApiResponse<OrganizationDetailResponse> getOrganizationDetail(
            @PathVariable Long organizationId
    ) {
        OrganizationDetailResponse response = queryService.getOrganizationDetail(organizationId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, response);
    }
}
