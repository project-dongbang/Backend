package com.dongbang.photo.infrastructure.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "app.aws.s3", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DefaultFileStorageService implements FileStorageService {

    @Override
    public FileStorageResult store(MultipartFile file, Long organizationId) {
        ImageFileValidator.ValidatedImage image = ImageFileValidator.validate(file);
        String uniqueFileName = UUID.randomUUID() + image.extension();
        String storageKey = String.format("organizations/%d/photos/%s", organizationId, uniqueFileName);

        return new FileStorageResult(
                storageKey,
                image.originalName(),
                image.contentType(),
                file.getSize(),
                image.checksum()
        );
    }

    @Override
    public String getFileUrl(String storageKey) {
        if (storageKey == null) {
            return null;
        }
        return "/uploads/" + storageKey;
    }

    @Override
    public void delete(String storageKey) {
        // Local/mock storage cleanup hook
    }
}
