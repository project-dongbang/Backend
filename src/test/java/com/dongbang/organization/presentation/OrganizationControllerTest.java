package com.dongbang.organization.presentation;

import com.dongbang.global.config.SecurityConfig;
import com.dongbang.global.config.WebMvcConfig;
import com.dongbang.global.security.ApiSecurityExceptionHandler;
import com.dongbang.organization.application.OrganizationCommandService;
import com.dongbang.organization.application.OrganizationQueryService;
import com.dongbang.organization.presentation.dto.request.CreateOrganizationRequest;
import com.dongbang.organization.presentation.dto.response.CreateOrganizationResponse;
import com.dongbang.organization.presentation.dto.response.OrganizationDetailResponse;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
                        .header("X-User-Id", "1")
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
                1L, "동방 개발팀", "dongbang-dev", "설명", null, 10L, Instant.now()
        );

        given(queryService.getOrganizationDetail(1L)).willReturn(response);

        mvc.perform(get("/api/v1/organizations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON_200_001"))
                .andExpect(jsonPath("$.result.name").value("동방 개발팀"));
    }
}
