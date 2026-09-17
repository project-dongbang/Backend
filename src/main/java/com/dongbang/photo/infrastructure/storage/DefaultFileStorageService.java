package com.dongbang.photo.infrastructure.storage;

import com.dongbang.global.exception.GeneralException;
import com.dongbang.photo.exception.PhotoErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class DefaultFileStorageService implements FileStorageService {

    @Override
    public FileStorageResult store(MultipartFile file, Long organizationId) {
        if (file == null || file.isEmpty()) {
            throw new GeneralException(PhotoErrorCode.FILE_EMPTY);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new GeneralException(PhotoErrorCode.INVALID_FILE_TYPE);
        }

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "photo.jpg";
        String extension = extractExtension(originalName);
        String uniqueFileName = UUID.randomUUID() + extension;
        String storageKey = String.format("organizations/%d/photos/%s", organizationId, uniqueFileName);

        String checksum;
        try {
            checksum = DigestUtils.md5DigestAsHex(file.getBytes());
        } catch (IOException e) {
            throw new GeneralException(PhotoErrorCode.FILE_UPLOAD_FAILED);
        }

        return new FileStorageResult(
                storageKey,
                originalName,
                contentType,
                file.getSize(),
                checksum
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

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(dotIndex) : ".jpg";
    }
}
