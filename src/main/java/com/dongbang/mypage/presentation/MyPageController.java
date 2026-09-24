package com.dongbang.mypage.presentation;

import com.dongbang.global.response.ApiResponse;
import com.dongbang.global.response.code.GeneralSuccessCode;
import com.dongbang.global.security.CurrentUserId;
import com.dongbang.mypage.application.MyPageService;
import com.dongbang.mypage.application.MyPageActivityService;
import com.dongbang.mypage.presentation.dto.request.UpdateMyProfileRequest;
import com.dongbang.mypage.presentation.dto.response.MyProfileResponse;
import com.dongbang.mypage.presentation.dto.response.MyActivitiesResponse;
import com.dongbang.mypage.presentation.dto.response.UpdateMyProfileResponse;
import com.dongbang.mypage.presentation.dto.response.ProfileImageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "마이페이지", description = "내 프로필 조회 및 수정 API")
public class MyPageController {

    private final MyPageService myPageService;
    private final MyPageActivityService myPageActivityService;

    @GetMapping("/api/v1/users/me")
    @Operation(summary = "내 프로필 조회", description = "내 기본 프로필과 OAuth 연결 정보, 선택 동아리의 회원 정보를 조회합니다.")
    public ApiResponse<MyProfileResponse> getMyProfile(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @Parameter(description = "선택 동아리 ID")
            @RequestParam(required = false) @Positive Long organizationId
    ) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                myPageService.getMyProfile(userId, organizationId)
        );
    }

    @GetMapping("/api/v1/users/me/activities")
    @Operation(summary = "내 활동 내역 조회", description = "선택 동아리의 행사 신청 수, 납부 상태와 신청 행사 목록을 조회합니다.")
    public ApiResponse<MyActivitiesResponse> getMyActivities(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @Parameter(description = "선택 동아리 ID", required = true)
            @RequestParam @Positive Long organizationId
    ) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                myPageActivityService.getMyActivities(userId, organizationId)
        );
    }

    @PatchMapping("/api/v1/users/me")
    @Operation(summary = "내 프로필 수정", description = "이름, 학번, 학과, 서비스 이메일 중 전달된 값을 수정합니다.")
    public ApiResponse<UpdateMyProfileResponse> updateMyProfile(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @Valid @RequestBody UpdateMyProfileRequest request
    ) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                myPageService.updateMyProfile(userId, request)
        );
    }

    @PostMapping(value = "/api/v1/users/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "프로필 이미지 등록·변경", description = "새 프로필 이미지를 등록하거나 기존 이미지를 교체합니다.")
    public ApiResponse<ProfileImageResponse> updateProfileImage(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @RequestPart("image") MultipartFile image
    ) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, myPageService.updateProfileImage(userId, image));
    }

    @PutMapping("/api/v1/users/me/profile-image/default")
    @Operation(summary = "기본 프로필 이미지로 변경", description = "커스텀 프로필 이미지를 해제합니다.")
    public ApiResponse<ProfileImageResponse> useDefaultProfileImage(
            @Parameter(hidden = true) @CurrentUserId Long userId
    ) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, myPageService.useDefaultProfileImage(userId));
    }
}
