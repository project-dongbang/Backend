package com.dongbang.photo.infrastructure.storage;

public record FileStorageResult(
        String storageKey,
        String originalName,
        String contentType,
        long sizeBytes,
        String checksum
) {
}
