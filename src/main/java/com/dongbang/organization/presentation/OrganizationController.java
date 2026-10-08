package com.dongbang.organization.presentation;

import com.dongbang.global.response.ApiResponse;
import com.dongbang.global.response.ApiErrorExamples;
import com.dongbang.global.response.code.GeneralErrorCode;
import com.dongbang.global.response.code.GeneralSuccessCode;
import com.dongbang.global.security.CurrentUserId;
import com.dongbang.organization.application.OrganizationCommandService;
import com.dongbang.organization.application.OrganizationQueryService;
import com.dongbang.organization.domain.MembershipStatus;
import com.dongbang.organization.exception.OrganizationErrorCode;
import com.dongbang.organization.presentation.dto.request.*;
import com.dongbang.organization.presentation.dto.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "동아리", description = "동아리 생성, 가입, 멤버 및 권한 관리 API")
@ApiErrorExamples(value = GeneralErrorCode.class, names = {"BAD_REQUEST", "VALIDATION_ERROR", "UNAUTHORIZED", "FORBIDDEN"})
public class OrganizationController {

    private final OrganizationCommandService commandService;
    private final OrganizationQueryService queryService;

    // 1. 동아리 생성
    @Operation(summary = "동아리 생성", description = "새 동아리를 만들고 요청한 사용자를 대표로 등록합니다.")
    @PostMapping("/api/v1/organizations")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiErrorExamples(value = OrganizationErrorCode.class, names = {"SLUG_ALREADY_EXISTS"})
    public ApiResponse<CreateOrganizationResponse> createOrganization(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @Valid @RequestBody CreateOrganizationRequest request
    ) {
        CreateOrganizationResponse response = commandService.createOrganization(userId, request);
        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, response);
    }

    // 2. 동아리 수정
    @Operation(summary = "동아리 정보 수정", description = "운영진은 기본 정보를 수정할 수 있습니다. 운영 학기(YYYY-1/2), 기본 회비, 계좌를 포함한 settings는 대표만 수정할 수 있으며 기존 납부 항목에는 소급 적용되지 않습니다.")
    @PatchMapping("/api/v1/organizations/{organizationId}")
    @ApiErrorExamples(value = OrganizationErrorCode.class, names = {"STAFF_REQUIRED", "OWNER_REQUIRED", "ORGANIZATION_NOT_FOUND"})
    public ApiResponse<Void> updateOrganization(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @Parameter(description = "동아리 ID") @PathVariable Long organizationId,
            @Valid @RequestBody UpdateOrganizationRequest request
    ) {
        commandService.updateOrganization(userId, organizationId, request);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }

    // 3. 동아리 삭제
    @Operation(summary = "동아리 삭제", description = "대표가 동아리를 삭제합니다.")
    @DeleteMapping("/api/v1/organizations/{organizationId}")
    @ApiErrorExamples(value = OrganizationErrorCode.class, names = {"OWNER_REQUIRED", "ORGANIZATION_NOT_FOUND"})
    public ApiResponse<Void> deleteOrganization(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @Parameter(description = "동아리 ID") @PathVariable Long organizationId
    ) {
        commandService.deleteOrganization(userId, organizationId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }

    // 4. 동아리 초대링크 생성
    @Operation(summary = "초대 링크 생성", description = "동아리 가입에 사용할 초대 링크를 생성합니다.")
    @PostMapping("/api/v1/organizations/{organizationId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiErrorExamples(value = OrganizationErrorCode.class, names = {"STAFF_REQUIRED", "ORGANIZATION_NOT_FOUND"})
    public ApiResponse<InvitationResponse> createInvitation(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @Parameter(description = "동아리 ID") @PathVariable Long organizationId,
            @Valid @RequestBody(required = false) CreateInvitationRequest request
    ) {
        InvitationResponse response = commandService.createInvitation(userId, organizationId, request);
        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, response);
    }

    // 5. 동아리 회원 목록 조회
    @Operation(summary = "동아리 회원 목록 조회", description = "상태 조건으로 필터링한 동아리 회원 목록을 조회합니다.")
    @GetMapping("/api/v1/organizations/{organizationId}/members")
    @ApiErrorExamples(value = OrganizationErrorCode.class, names = {"STAFF_REQUIRED"})
    public ApiResponse<MemberListResponse> getMembers(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @Parameter(description = "동아리 ID") @PathVariable Long organizationId,
            @Parameter(description = "회원 상태 필터") @RequestParam(required = false) MembershipStatus status
    ) {
        MemberListResponse response = queryService.getMembers(userId, organizationId, status);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, response);
    }

    @Operation(summary = "멤버 운영 정보 수정", description = "활동 상태(ACTIVE/INACTIVE), 기수, 직책을 부분 수정합니다. 빈 직책은 null로 초기화합니다. 탈퇴·강퇴, 역할 변경은 각각 전용 API를 사용합니다.")
    @PatchMapping("/api/v1/organizations/{organizationId}/members/{memberId}")
    @ApiErrorExamples(value = OrganizationErrorCode.class,
            names = {"STAFF_REQUIRED", "OWNER_REQUIRED", "ORGANIZATION_NOT_FOUND", "MEMBER_NOT_FOUND",
                    "MEMBER_STATUS_NOT_EDITABLE", "OWNER_CANNOT_DEACTIVATE"})
    public ApiResponse<MemberItemResponse> updateMemberInfo(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @Parameter(description = "동아리 ID") @PathVariable Long organizationId,
            @Parameter(description = "멤버십 ID") @PathVariable Long memberId,
            @Valid @RequestBody UpdateMemberInfoRequest request
    ) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK,
                commandService.updateMemberInfo(userId, organizationId, memberId, request));
    }

    // 6. 동아리 회원 권한 변경
    @Operation(summary = "회원 권한 변경", description = "대표 또는 권한이 있는 운영진이 회원의 역할을 변경합니다.")
    @PatchMapping("/api/v1/organizations/{organizationId}/members/{memberId}/role")
    @ApiErrorExamples(value = OrganizationErrorCode.class, names = {"OWNER_REQUIRED", "MEMBER_NOT_FOUND", "INVALID_DELEGATION_TARGET"})
    public ApiResponse<Void> changeMemberRole(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @Parameter(description = "동아리 ID") @PathVariable Long organizationId,
            @Parameter(description = "멤버십 ID") @PathVariable Long memberId,
            @Valid @RequestBody ChangeRoleRequest request
    ) {
        commandService.changeMemberRole(userId, organizationId, memberId, request);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }

    // 7. 동아리 강퇴
    @Operation(summary = "회원 강퇴", description = "대표 또는 권한이 있는 운영진이 동아리 회원을 강퇴합니다.")
    @DeleteMapping("/api/v1/organizations/{organizationId}/members/{memberId}")
    @ApiErrorExamples(value = OrganizationErrorCode.class, names = {"OWNER_REQUIRED", "MEMBER_NOT_FOUND", "INVALID_DELEGATION_TARGET"})
    public ApiResponse<Void> expelMember(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @Parameter(description = "동아리 ID") @PathVariable Long organizationId,
            @Parameter(description = "멤버십 ID") @PathVariable Long memberId
    ) {
        commandService.expelMember(userId, organizationId, memberId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }

    // 8. 대표 권한 위임
    @Operation(summary = "대표 권한 위임", description = "현재 대표가 다른 회원에게 대표 권한을 위임합니다.")
    @PatchMapping("/api/v1/organizations/{organizationId}/owner")
    @ApiErrorExamples(value = OrganizationErrorCode.class, names = {"OWNER_REQUIRED", "MEMBER_NOT_FOUND", "INVALID_DELEGATION_TARGET"})
    public ApiResponse<Void> delegateOwner(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @Parameter(description = "동아리 ID") @PathVariable Long organizationId,
            @Valid @RequestBody DelegateOwnerRequest request
    ) {
        commandService.delegateOwner(userId, organizationId, request);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }

    // 9. 동아리 참여(코드)
    @Operation(summary = "초대 링크로 동아리 가입", description = "유효한 초대 토큰으로 동아리에 가입합니다.")
    @PostMapping("/api/v1/invitations/{token}/accept")
    @ApiErrorExamples(value = OrganizationErrorCode.class, names = {"INVALID_INVITATION_TOKEN", "ORGANIZATION_NOT_FOUND", "ALREADY_JOINED_MEMBER"})
    public ApiResponse<JoinOrganizationResponse> acceptInvitation(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @Parameter(description = "초대 토큰") @PathVariable String token
    ) {
        JoinOrganizationResponse response = commandService.acceptInvitation(userId, token);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, response);
    }

    // 10. 동아리 탈퇴
    @Operation(summary = "동아리 탈퇴", description = "현재 사용자가 동아리에서 탈퇴합니다.")
    @DeleteMapping("/api/v1/organizations/{organizationId}/members/me")
    @ApiErrorExamples(value = OrganizationErrorCode.class, names = {"MEMBER_NOT_FOUND", "OWNER_CANNOT_LEAVE"})
    public ApiResponse<Void> leaveOrganization(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @Parameter(description = "동아리 ID") @PathVariable Long organizationId
    ) {
        commandService.leaveOrganization(userId, organizationId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }

    // 11. 내 참여 동아리 목록 조회
    @Operation(summary = "내 참여 동아리 목록 조회", description = "현재 사용자가 가입한 동아리 목록을 조회합니다.")
    @GetMapping("/api/v1/users/me/organizations")
    public ApiResponse<MyOrganizationResponse> getMyOrganizations(
            @Parameter(hidden = true) @CurrentUserId Long userId
    ) {
        MyOrganizationResponse response = queryService.getMyOrganizations(userId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, response);
    }

    // 12. 동아리 상세조회
    @Operation(summary = "동아리 상세 조회", description = "동아리 기본 정보와 운영 설정을 조회합니다. 설정하지 않은 학기·회비·계좌는 null입니다.")
    @GetMapping("/api/v1/organizations/{organizationId}")
    @ApiErrorExamples(value = OrganizationErrorCode.class, names = {"MEMBER_REQUIRED", "ORGANIZATION_NOT_FOUND"})
    public ApiResponse<OrganizationDetailResponse> getOrganizationDetail(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @Parameter(description = "동아리 ID") @PathVariable Long organizationId
    ) {
        OrganizationDetailResponse response = queryService.getOrganizationDetail(organizationId, userId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, response);
    }
}
