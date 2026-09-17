package com.dongbang.photo.presentation.dto.request;

import jakarta.validation.constraints.Size;

public record UpdatePhotoRequest(
        @Size(max = 200, message = "제목은 200자 이내여야 합니다.")
        String title
) {
}
