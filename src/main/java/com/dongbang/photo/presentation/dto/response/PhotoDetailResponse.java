package com.dongbang.photo.presentation.dto.response;

import java.time.Instant;

public record PhotoDetailResponse(
        Long photoId,
        Long organizationId,
        String title,
        FileInfo file,
        UploaderInfo uploader,
        Instant createdAt,
        Instant updatedAt
) {
}
