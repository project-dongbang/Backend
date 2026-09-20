package com.dongbang.photo.presentation;

import com.dongbang.global.response.ApiResponse;
import com.dongbang.global.response.code.GeneralSuccessCode;
import com.dongbang.global.security.CurrentUserId;
import com.dongbang.photo.application.PhotoCommandService;
import com.dongbang.photo.application.PhotoQueryService;
import com.dongbang.photo.presentation.dto.request.CreatePhotoRequest;
import com.dongbang.photo.presentation.dto.request.UpdatePhotoRequest;
import com.dongbang.photo.presentation.dto.response.PhotoCursorResponse;
import com.dongbang.photo.presentation.dto.response.PhotoDetailResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Tag(name = "사진첩", description = "동아리 사진첩 관리 API")
public class PhotoController {

    private final PhotoCommandService commandService;
    private final PhotoQueryService queryService;

    // 1. 사진첩 목록 조회 (No-Offset 커서 기반 무한 스크롤)
    @GetMapping("/api/v1/organizations/{organizationId}/photos")
    @Operation(summary = "사진 목록 조회", description = "동아리 사진을 커서 기반으로 조회합니다.")
    public ApiResponse<PhotoCursorResponse> getPhotos(
            @CurrentUserId Long userId,
            @PathVariable Long organizationId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        PhotoCursorResponse response = queryService.getPhotosCursor(organizationId, userId, cursor, size);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, response);
    }

    // 2. 사진첩 사진 상세 조회
    @GetMapping("/api/v1/organizations/{organizationId}/photos/{photoId}")
    @Operation(summary = "사진 상세 조회", description = "동아리 사진의 상세 정보를 조회합니다.")
    public ApiResponse<PhotoDetailResponse> getPhotoDetail(
            @CurrentUserId Long userId,
            @PathVariable Long organizationId,
            @PathVariable Long photoId
    ) {
        PhotoDetailResponse response = queryService.getPhotoDetail(organizationId, userId, photoId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, response);
    }

    // 3. 사진첩 사진 등록 (Multipart 직접 업로드)
    @PostMapping(value = "/api/v1/organizations/{organizationId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "사진 업로드", description = "이미지 파일을 업로드해 동아리 사진첩에 등록합니다.")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PhotoDetailResponse> uploadPhoto(
            @CurrentUserId Long userId,
            @PathVariable Long organizationId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title
    ) {
        PhotoDetailResponse response = commandService.createPhoto(organizationId, userId, file, title);
        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, response);
    }

    // 3-1. 사진첩 사진 등록 (기 업로드 파일 ID 연결 등록)
    @PostMapping(value = "/api/v1/organizations/{organizationId}/photos", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "업로드된 파일로 사진 등록", description = "기존 업로드 파일 ID를 연결해 사진첩에 등록합니다.")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PhotoDetailResponse> createPhotoWithFileId(
            @CurrentUserId Long userId,
            @PathVariable Long organizationId,
            @Valid @RequestBody CreatePhotoRequest request
    ) {
        PhotoDetailResponse response = commandService.createPhotoWithFileId(organizationId, userId, request.uploadedFileId(), request.title());
        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, response);
    }

    // 4. 사진첩 사진 정보 수정
    @PatchMapping("/api/v1/organizations/{organizationId}/photos/{photoId}")
    @Operation(summary = "사진 정보 수정", description = "사진 제목 등 사진 정보를 수정합니다.")
    public ApiResponse<PhotoDetailResponse> updatePhoto(
            @CurrentUserId Long userId,
            @PathVariable Long organizationId,
            @PathVariable Long photoId,
            @Valid @RequestBody UpdatePhotoRequest request
    ) {
        PhotoDetailResponse response = commandService.updatePhoto(organizationId, userId, photoId, request);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, response);
    }

    // 5. 사진첩 사진 삭제
    @DeleteMapping("/api/v1/organizations/{organizationId}/photos/{photoId}")
    @Operation(summary = "사진 삭제", description = "동아리 사진첩에서 사진을 삭제합니다.")
    public ApiResponse<Void> deletePhoto(
            @CurrentUserId Long userId,
            @PathVariable Long organizationId,
            @PathVariable Long photoId
    ) {
        commandService.deletePhoto(organizationId, userId, photoId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }
}
