package com.dongbang.photo.presentation.dto.response;

public record FileInfo(
        Long uploadedFileId,
        String storageKey,
        String originalName,
        String contentType,
        Long sizeBytes,
        String url
) {
}
