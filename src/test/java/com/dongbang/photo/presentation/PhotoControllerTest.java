package com.dongbang.photo.presentation;

import com.dongbang.global.config.SecurityConfig;
import com.dongbang.global.config.WebMvcConfig;
import com.dongbang.global.security.ApiSecurityExceptionHandler;
import com.dongbang.photo.application.PhotoCommandService;
import com.dongbang.photo.application.PhotoQueryService;
import com.dongbang.photo.presentation.dto.request.UpdatePhotoRequest;
import com.dongbang.photo.presentation.dto.response.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PhotoController.class)
@Import({SecurityConfig.class, ApiSecurityExceptionHandler.class, WebMvcConfig.class})
class PhotoControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PhotoCommandService commandService;

    @MockitoBean
    private PhotoQueryService queryService;

    private final Long orgId = 1L;

    @Test
    @DisplayName("사진첩 목록 조회 API 호출 성공")
    void getPhotos() throws Exception {
        PhotoListItemResponse item = new PhotoListItemResponse(
                10L, "MT 단체사진", "/uploads/key.jpg", 1L,
                new UploaderInfo(5L, "홍길동"), Instant.now()
        );
        PhotoCursorResponse response = new PhotoCursorResponse(List.of(item), true, 9L);

        given(queryService.getPhotosCursor(eq(orgId), eq(1L), eq(15L), eq(20)))
                .willReturn(response);

        mvc.perform(get("/api/v1/organizations/{organizationId}/photos", orgId)
                        .header("X-User-Id", "1")
                        .param("cursor", "15")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.hasNext").value(true))
                .andExpect(jsonPath("$.result.nextCursor").value(9))
                .andExpect(jsonPath("$.result.content[0].photoId").value(10))
                .andExpect(jsonPath("$.result.content[0].title").value("MT 단체사진"));
    }

    @Test
    @DisplayName("사진 상세 조회 API 호출 성공")
    void getPhotoDetail() throws Exception {
        Long photoId = 10L;
        FileInfo fileInfo = new FileInfo(1L, "storageKey", "original.jpg", "image/jpeg", 1024L, "/uploads/storageKey");
        UploaderInfo uploaderInfo = new UploaderInfo(5L, "홍길동");
        PhotoDetailResponse response = new PhotoDetailResponse(
                photoId, orgId, "사진 제목", fileInfo, uploaderInfo, Instant.now(), Instant.now()
        );

        given(queryService.getPhotoDetail(eq(orgId), eq(1L), eq(photoId)))
                .willReturn(response);

        mvc.perform(get("/api/v1/organizations/{organizationId}/photos/{photoId}", orgId, photoId)
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.photoId").value(10))
                .andExpect(jsonPath("$.result.file.originalName").value("original.jpg"))
                .andExpect(jsonPath("$.result.uploader.memberName").value("홍길동"));
    }

    @Test
    @DisplayName("사진 업로드 등록 API 호출 성공")
    void uploadPhoto() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "mt.jpg", "image/jpeg", "dummy-content".getBytes()
        );
        FileInfo fileInfo = new FileInfo(1L, "storageKey", "mt.jpg", "image/jpeg", 1024L, "/uploads/storageKey");
        UploaderInfo uploaderInfo = new UploaderInfo(5L, "홍길동");
        PhotoDetailResponse response = new PhotoDetailResponse(
                20L, orgId, "새 사진", fileInfo, uploaderInfo, Instant.now(), Instant.now()
        );

        given(commandService.createPhoto(eq(orgId), eq(1L), any(), eq("새 사진")))
                .willReturn(response);

        mvc.perform(multipart("/api/v1/organizations/{organizationId}/photos", orgId)
                        .file(file)
                        .param("title", "새 사진")
                        .header("X-User-Id", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.photoId").value(20))
                .andExpect(jsonPath("$.result.title").value("새 사진"));
    }

    @Test
    @DisplayName("사진 수정 API 호출 성공")
    void updatePhoto() throws Exception {
        Long photoId = 20L;
        UpdatePhotoRequest request = new UpdatePhotoRequest("수정된 제목");
        FileInfo fileInfo = new FileInfo(1L, "storageKey", "mt.jpg", "image/jpeg", 1024L, "/uploads/storageKey");
        UploaderInfo uploaderInfo = new UploaderInfo(5L, "홍길동");
        PhotoDetailResponse response = new PhotoDetailResponse(
                photoId, orgId, "수정된 제목", fileInfo, uploaderInfo, Instant.now(), Instant.now()
        );

        given(commandService.updatePhoto(eq(orgId), eq(1L), eq(photoId), any(UpdatePhotoRequest.class)))
                .willReturn(response);

        mvc.perform(patch("/api/v1/organizations/{organizationId}/photos/{photoId}", orgId, photoId)
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.title").value("수정된 제목"));
    }

    @Test
    @DisplayName("사진 삭제 API 호출 성공")
    void deletePhoto() throws Exception {
        Long photoId = 20L;

        mvc.perform(delete("/api/v1/organizations/{organizationId}/photos/{photoId}", orgId, photoId)
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
    }
}
