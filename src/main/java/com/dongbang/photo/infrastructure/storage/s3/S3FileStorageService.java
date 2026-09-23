package com.dongbang.photo.infrastructure.storage.s3;

import com.dongbang.global.exception.GeneralException;
import com.dongbang.photo.exception.PhotoErrorCode;
import com.dongbang.photo.infrastructure.storage.FileStorageResult;
import com.dongbang.photo.infrastructure.storage.FileStorageService;
import com.dongbang.photo.infrastructure.storage.ImageFileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.aws.s3", name = "enabled", havingValue = "true")
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

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

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(storageKey)
                    .contentType(image.contentType())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(image.bytes()));
            log.info("Successfully uploaded photo to S3: bucket={}, key={}", s3Properties.getBucket(), storageKey);
        } catch (Exception e) {
            log.error("Failed to upload photo to S3: bucket={}, key={}", s3Properties.getBucket(), storageKey, e);
            throw new GeneralException(PhotoErrorCode.FILE_UPLOAD_FAILED);
        }

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
        if (!StringUtils.hasText(storageKey)) {
            return null;
        }
        if (StringUtils.hasText(s3Properties.getCustomDomain())) {
            String domain = s3Properties.getCustomDomain().replaceAll("/$", "");
            return String.format("%s/%s", domain, storageKey);
        }
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(storageKey)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(s3Properties.getUrlTtl())
                .getObjectRequest(getRequest)
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    @Override
    public void delete(String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            return;
        }
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(storageKey)
                    .build();
            s3Client.deleteObject(deleteRequest);
            log.info("Deleted photo from S3: bucket={}, key={}", s3Properties.getBucket(), storageKey);
        } catch (Exception e) {
            log.warn("Failed to delete photo from S3: bucket={}, key={}", s3Properties.getBucket(), storageKey, e);
        }
    }
}
