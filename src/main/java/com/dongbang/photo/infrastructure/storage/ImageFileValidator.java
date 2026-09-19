package com.dongbang.photo.infrastructure.storage;

import com.dongbang.global.exception.GeneralException;
import com.dongbang.photo.exception.PhotoErrorCode;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class ImageFileValidator {

    private ImageFileValidator() {
    }

    public static ValidatedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new GeneralException(PhotoErrorCode.FILE_EMPTY);
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new GeneralException(PhotoErrorCode.FILE_UPLOAD_FAILED);
        }

        ImageType detectedType = detect(bytes);
        String declaredType = normalizeContentType(file.getContentType());
        if (detectedType == null || !detectedType.contentType().equals(declaredType)) {
            throw new GeneralException(PhotoErrorCode.INVALID_FILE_TYPE);
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            originalName = "photo" + detectedType.extension();
        }

        return new ValidatedImage(
                originalName,
                bytes,
                detectedType.contentType(),
                detectedType.extension(),
                DigestUtils.md5DigestAsHex(bytes)
        );
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null) {
            return null;
        }
        String normalized = contentType.toLowerCase(Locale.ROOT);
        return "image/jpg".equals(normalized) ? "image/jpeg" : normalized;
    }

    private static ImageType detect(byte[] bytes) {
        if (startsWith(bytes, 0xFF, 0xD8, 0xFF)) {
            return new ImageType("image/jpeg", ".jpg");
        }
        if (startsWith(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return new ImageType("image/png", ".png");
        }
        if (bytes.length >= 12
                && "RIFF".equals(new String(bytes, 0, 4, StandardCharsets.US_ASCII))
                && "WEBP".equals(new String(bytes, 8, 4, StandardCharsets.US_ASCII))) {
            return new ImageType("image/webp", ".webp");
        }
        return null;
    }

    private static boolean startsWith(byte[] bytes, int... signature) {
        if (bytes.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((bytes[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private record ImageType(String contentType, String extension) {
    }

    public record ValidatedImage(
            String originalName,
            byte[] bytes,
            String contentType,
            String extension,
            String checksum
    ) {
    }
}
