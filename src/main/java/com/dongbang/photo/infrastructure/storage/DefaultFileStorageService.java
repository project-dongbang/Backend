package com.dongbang.photo.infrastructure.storage;

import com.dongbang.global.exception.GeneralException;
import com.dongbang.photo.exception.PhotoErrorCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "app.aws.s3", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DefaultFileStorageService implements FileStorageService {

    private final Path rootDirectory;

    public DefaultFileStorageService(@Value("${app.storage.local-dir}") String localDirectory) {
        this.rootDirectory = Path.of(localDirectory).toAbsolutePath().normalize();
    }

    @Override
    public FileStorageResult store(MultipartFile file, Long organizationId) {
        return store(file, organizationId, "photos");
    }

    @Override
    public FileStorageResult store(MultipartFile file, Long organizationId, String directory) {
        return storeAt(file, String.format("organizations/%d/%s", organizationId, directory));
    }

    @Override
    public FileStorageResult storeForUser(MultipartFile file, Long userId, String directory) {
        return storeAt(file, String.format("users/%d/%s", userId, directory));
    }

    private FileStorageResult storeAt(MultipartFile file, String prefix) {
        ImageFileValidator.ValidatedImage image = ImageFileValidator.validate(file);
        String uniqueFileName = UUID.randomUUID() + image.extension();
        String storageKey = prefix + "/" + uniqueFileName;

        Path target = rootDirectory.resolve(storageKey).normalize();
        if (!target.startsWith(rootDirectory)) {
            throw new GeneralException(PhotoErrorCode.FILE_UPLOAD_FAILED);
        }
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, image.bytes());
        } catch (IOException ex) {
            throw new GeneralException(PhotoErrorCode.FILE_UPLOAD_FAILED);
        }

        return new FileStorageResult(storageKey, image.originalName(), image.contentType(),
                file.getSize(), image.checksum());
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
        if (storageKey == null) {
            return;
        }
        try {
            Files.deleteIfExists(rootDirectory.resolve(storageKey).normalize());
        } catch (IOException ignored) {
            // Cleanup is best effort; the database state remains authoritative.
        }
    }
}
