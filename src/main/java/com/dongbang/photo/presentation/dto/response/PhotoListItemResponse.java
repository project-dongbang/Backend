package com.dongbang.photo.presentation.dto.response;

import java.time.Instant;

public record PhotoListItemResponse(
        Long photoId,
        String title,
        String imageUrl,
        Long uploadedFileId,
        UploaderInfo uploader,
        Instant createdAt
) {
}
