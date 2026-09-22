package com.dongbang.mypage.presentation;

import com.dongbang.auth.domain.OAuthProvider;
import com.dongbang.global.config.SecurityConfig;
import com.dongbang.global.config.WebMvcConfig;
import com.dongbang.global.security.ApiSecurityExceptionHandler;
import com.dongbang.mypage.application.MyPageService;
import com.dongbang.mypage.presentation.dto.response.CurrentMembershipResponse;
import com.dongbang.mypage.presentation.dto.response.MyProfileResponse;
import com.dongbang.mypage.presentation.dto.response.UpdateMyProfileResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MyPageController.class)
@Import({SecurityConfig.class, ApiSecurityExceptionHandler.class, WebMvcConfig.class})
class MyPageControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private MyPageService myPageService;

    @Test
    @DisplayName("내 프로필과 선택 동아리 회원 정보를 조회한다")
    void getMyProfile() throws Exception {
        given(myPageService.getMyProfile(1L, 10L)).willReturn(new MyProfileResponse(
                1L,
                "김동방",
                "20260004",
                "컴퓨터정보공학부",
                "dongbang@example.com",
                null,
                List.of(OAuthProvider.GOOGLE),
                new CurrentMembershipResponse(10L, "MANAGER", "11기", "총무")
        ));

        mvc.perform(get("/api/v1/users/me")
                        .queryParam("organizationId", "10")
                        .with(user("1").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON_200_001"))
                .andExpect(jsonPath("$.result.userId").value(1))
                .andExpect(jsonPath("$.result.profileImageUrl").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.result.oauthProviders[0]").value("GOOGLE"))
                .andExpect(jsonPath("$.result.currentMembership.role").value("MANAGER"))
                .andExpect(jsonPath("$.result.currentMembership.generation").value("11기"));
    }

    @Test
    @DisplayName("내 프로필의 전달된 필드를 수정한다")
    void updateMyProfile() throws Exception {
        given(myPageService.updateMyProfile(any(), any())).willReturn(new UpdateMyProfileResponse(
                1L,
                "김동방",
                "20260004",
                "소프트웨어학과",
                "new@example.com",
                Instant.parse("2026-09-22T10:00:00Z")
        ));

        mvc.perform(patch("/api/v1/users/me")
                        .with(user("1").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "department": "소프트웨어학과",
                                  "email": "new@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.department").value("소프트웨어학과"))
                .andExpect(jsonPath("$.result.email").value("new@example.com"))
                .andExpect(jsonPath("$.result.updatedAt").value("2026-09-22T10:00:00Z"));
    }

    @Test
    @DisplayName("수정할 필드가 없으면 검증 오류를 반환한다")
    void updateMyProfileRequiresAtLeastOneField() throws Exception {
        mvc.perform(patch("/api/v1/users/me")
                        .with(user("1").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_002"))
                .andExpect(jsonPath("$.result").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    @DisplayName("동아리 ID는 양수여야 한다")
    void getMyProfileValidatesOrganizationId() throws Exception {
        mvc.perform(get("/api/v1/users/me")
                        .queryParam("organizationId", "0")
                        .with(user("1").roles("USER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_002"));
    }
}
