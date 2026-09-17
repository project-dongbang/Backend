package com.dongbang.photo.application.facade;

import com.dongbang.photo.domain.Photo;
import com.dongbang.photo.domain.UploadedFile;
import com.dongbang.photo.domain.repository.PhotoRepository;
import com.dongbang.photo.domain.repository.UploadedFileRepository;
import com.dongbang.photo.infrastructure.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PhotoAccessFacade {

    private final PhotoRepository photoRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final FileStorageService fileStorageService;

    public List<PhotoSummary> getRecentPhotos(Long organizationId, int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }

        List<Photo> photos = photoRepository.findPhotosByCursor(
                organizationId,
                null,
                PageRequest.of(0, limit)
        );

        if (photos.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> fileIds = photos.stream()
                .map(Photo::getUploadedFileId)
                .collect(Collectors.toSet());

        Map<Long, UploadedFile> fileMap = uploadedFileRepository
                .findAllByIdInAndOrganizationIdAndDeletedAtIsNull(fileIds, organizationId).stream()
                .collect(Collectors.toMap(UploadedFile::getId, f -> f));

        return photos.stream().map(p -> {
            UploadedFile file = fileMap.get(p.getUploadedFileId());
            String imageUrl = file != null ? fileStorageService.getFileUrl(file.getStorageKey()) : null;
            return new PhotoSummary(p.getId(), p.getTitle(), imageUrl);
        }).toList();
    }
}
