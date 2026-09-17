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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoCommandService commandService;
    private final PhotoQueryService queryService;

    // 1. 사진첩 목록 조회 (No-Offset 커서 기반 무한 스크롤)
    @GetMapping("/api/v1/organizations/{organizationId}/photos")
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
    public ApiResponse<Void> deletePhoto(
            @CurrentUserId Long userId,
            @PathVariable Long organizationId,
            @PathVariable Long photoId
    ) {
        commandService.deletePhoto(organizationId, userId, photoId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }
}
