package com.dongbang.photo.presentation.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

public record CreatePhotoRequest(
        @Size(max = 200, message = "제목은 200자 이내여야 합니다.")
        String title,
        @NotNull(message = "업로드 파일 ID는 필수입니다.")
        Long uploadedFileId
) {
}
