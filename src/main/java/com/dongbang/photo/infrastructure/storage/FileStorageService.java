package com.dongbang.photo.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    FileStorageResult store(MultipartFile file, Long organizationId);
    FileStorageResult store(MultipartFile file, Long organizationId, String directory);
    FileStorageResult storeForUser(MultipartFile file, Long userId, String directory);
    String getFileUrl(String storageKey);
    void delete(String storageKey);
}
