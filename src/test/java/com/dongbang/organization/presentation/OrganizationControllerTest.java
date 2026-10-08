package com.dongbang.organization.presentation;

import com.dongbang.global.config.SecurityConfig;
import com.dongbang.global.config.WebMvcConfig;
import com.dongbang.global.security.ApiSecurityExceptionHandler;
import com.dongbang.organization.application.OrganizationCommandService;
import com.dongbang.organization.application.OrganizationQueryService;
import com.dongbang.organization.presentation.dto.request.CreateOrganizationRequest;
import com.dongbang.organization.presentation.dto.request.UpdateMemberInfoRequest;
import com.dongbang.organization.presentation.dto.request.UpdateOrganizationRequest;
import com.dongbang.organization.presentation.dto.request.UpdateOrganizationSettingsRequest;
import com.dongbang.organization.presentation.dto.OrganizationPaymentAccount;
import com.dongbang.organization.presentation.dto.response.CreateOrganizationResponse;
import com.dongbang.organization.presentation.dto.response.MemberItemResponse;
import com.dongbang.organization.presentation.dto.response.OrganizationDetailResponse;
import com.dongbang.organization.presentation.dto.response.OrganizationSettingsResponse;
import com.dongbang.organization.domain.MembershipRole;
import com.dongbang.organization.domain.MembershipStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrganizationController.class)
@Import({SecurityConfig.class, ApiSecurityExceptionHandler.class, WebMvcConfig.class})
class OrganizationControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrganizationCommandService commandService;

    @MockitoBean
    private OrganizationQueryService queryService;

    @Test
    @DisplayName("동아리 생성 API 호출 성공")
    void createOrganization() throws Exception {
        CreateOrganizationRequest request = new CreateOrganizationRequest(
                "동방 개발팀", "dongbang-dev", "설명", null
        );
        CreateOrganizationResponse response = new CreateOrganizationResponse(1L, "dongbang-dev");

        given(commandService.createOrganization(eq(1L), any(CreateOrganizationRequest.class)))
                .willReturn(response);

        mvc.perform(post("/api/v1/organizations")
                        .with(user("1").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON_201_001"))
                .andExpect(jsonPath("$.result.organizationId").value(1))
                .andExpect(jsonPath("$.result.slug").value("dongbang-dev"));
    }

    @Test
    @DisplayName("동아리 상세 조회 API 호출 성공")
    void getOrganizationDetail() throws Exception {
        OrganizationDetailResponse response = new OrganizationDetailResponse(
                1L, "동방 개발팀", "dongbang-dev", "설명", null, 10L, Instant.now(),
                new OrganizationSettingsResponse("2026-2", new BigDecimal("40000"),
                        new OrganizationPaymentAccount("국민은행", "123-456", "동방"))
        );

        given(queryService.getOrganizationDetail(1L, 1L)).willReturn(response);

        mvc.perform(get("/api/v1/organizations/1")
                        .with(user("1").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON_200_001"))
                .andExpect(jsonPath("$.result.name").value("동방 개발팀"))
                .andExpect(jsonPath("$.result.settings.operatingSemester").value("2026-2"))
                .andExpect(jsonPath("$.result.settings.defaultFeeAmount").value(40000))
                .andExpect(jsonPath("$.result.settings.paymentAccount.bankName").value("국민은행"));
    }

    @Test
    @DisplayName("운영 설정 수정 요청의 중첩 계좌 필드를 바인딩한다")
    void updateOrganizationSettings() throws Exception {
        UpdateOrganizationRequest request = new UpdateOrganizationRequest(null, null, null,
                new UpdateOrganizationSettingsRequest("2026-2", new BigDecimal("40000"),
                        new OrganizationPaymentAccount("국민은행", "123-456", "동방")));

        mvc.perform(patch("/api/v1/organizations/1")
                        .with(user("1").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("운영 설정의 불완전한 계좌는 거부한다")
    void updateOrganizationSettings_invalidAccount() throws Exception {
        UpdateOrganizationRequest request = new UpdateOrganizationRequest(null, null, null,
                new UpdateOrganizationSettingsRequest(null, null,
                        new OrganizationPaymentAccount("", "123-456", "동방")));

        mvc.perform(patch("/api/v1/organizations/1")
                        .with(user("1").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("멤버 운영 정보 수정 요청과 응답")
    void updateMemberInfo() throws Exception {
        MemberItemResponse response = new MemberItemResponse(7L, 8L, "홍길동", "20240001", "13기",
                "총무", MembershipRole.ADMIN, MembershipStatus.INACTIVE, Instant.now());
        given(commandService.updateMemberInfo(eq(1L), eq(1L), eq(7L), any(UpdateMemberInfoRequest.class)))
                .willReturn(response);

        mvc.perform(patch("/api/v1/organizations/1/members/7")
                        .with(user("1").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\",\"generation\":\"13기\",\"position\":\"총무\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("INACTIVE"))
                .andExpect(jsonPath("$.result.position").value("총무"));
    }

    @Test
    @DisplayName("멤버 활동 상태에 탈퇴 값을 직접 지정할 수 없다")
    void updateMemberInfo_rejectsLeft() throws Exception {
        mvc.perform(patch("/api/v1/organizations/1/members/7")
                        .with(user("1").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"LEFT\"}"))
                .andExpect(status().isBadRequest());
    }
}
