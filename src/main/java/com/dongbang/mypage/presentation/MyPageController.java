package com.dongbang.mypage.presentation;

import com.dongbang.global.response.ApiResponse;
import com.dongbang.global.response.code.GeneralSuccessCode;
import com.dongbang.global.security.CurrentUserId;
import com.dongbang.mypage.application.MyPageService;
import com.dongbang.mypage.presentation.dto.request.UpdateMyProfileRequest;
import com.dongbang.mypage.presentation.dto.response.MyProfileResponse;
import com.dongbang.mypage.presentation.dto.response.UpdateMyProfileResponse;
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

@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "마이페이지", description = "내 프로필 조회 및 수정 API")
public class MyPageController {

    private final MyPageService myPageService;

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
}
