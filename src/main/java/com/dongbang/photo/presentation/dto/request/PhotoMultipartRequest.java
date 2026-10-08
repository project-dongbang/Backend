package com.dongbang.photo.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

public record PhotoMultipartRequest(
        @Schema(type = "string", format = "binary", requiredMode = Schema.RequiredMode.REQUIRED,
                description = "JPEG, PNG 또는 WebP 이미지 파일 (최대 10MB)")
        MultipartFile file,
        @Schema(maxLength = 200, description = "선택적 사진 제목")
        String title
) {
}
