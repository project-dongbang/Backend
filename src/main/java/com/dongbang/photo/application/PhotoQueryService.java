package com.dongbang.photo.application;

import com.dongbang.global.exception.GeneralException;
import com.dongbang.organization.application.facade.MembershipAccessFacade;
import com.dongbang.organization.application.facade.MembershipSummary;
import com.dongbang.photo.domain.Photo;
import com.dongbang.photo.domain.UploadedFile;
import com.dongbang.photo.domain.repository.PhotoRepository;
import com.dongbang.photo.domain.repository.UploadedFileRepository;
import com.dongbang.photo.exception.PhotoErrorCode;
import com.dongbang.photo.infrastructure.storage.FileStorageService;
import com.dongbang.photo.presentation.dto.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PhotoQueryService {

    private final PhotoRepository photoRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final FileStorageService fileStorageService;
    private final MembershipAccessFacade membershipAccessFacade;

    public PhotoCursorResponse getPhotosCursor(Long organizationId, Long userId, Long cursor, int size) {
        validateActiveMember(organizationId, userId);

        int querySize = size <= 0 ? 20 : Math.min(size, 100);
        List<Photo> photos = photoRepository.findPhotosByCursor(
                organizationId,
                cursor,
                PageRequest.of(0, querySize + 1)
        );

        boolean hasNext = photos.size() > querySize;
        List<Photo> content = hasNext ? photos.subList(0, querySize) : photos;
        Long nextCursor = hasNext && !content.isEmpty() ? content.get(content.size() - 1).getId() : null;

        if (content.isEmpty()) {
            return new PhotoCursorResponse(List.of(), false, null);
        }

        Set<Long> fileIds = content.stream().map(Photo::getUploadedFileId).collect(Collectors.toSet());
        Map<Long, UploadedFile> fileMap = uploadedFileRepository
                .findAllByIdInAndOrganizationIdAndDeletedAtIsNull(fileIds, organizationId).stream()
                .collect(Collectors.toMap(UploadedFile::getId, f -> f));

        Set<Long> membershipIds = content.stream().map(Photo::getMembershipId).collect(Collectors.toSet());
        Map<Long, MembershipSummary> membershipMap = membershipAccessFacade.getMembershipSummariesByIds(membershipIds);

        List<PhotoListItemResponse> items = content.stream().map(photo -> {
            UploadedFile file = fileMap.get(photo.getUploadedFileId());
            String imageUrl = file != null ? fileStorageService.getFileUrl(file.getStorageKey()) : null;
            MembershipSummary member = membershipMap.get(photo.getMembershipId());
            String memberName = member != null ? member.memberName() : "알 수 없음";

            return new PhotoListItemResponse(
                    photo.getId(),
                    photo.getTitle(),
                    imageUrl,
                    photo.getUploadedFileId(),
                    new UploaderInfo(photo.getMembershipId(), memberName),
                    photo.getCreatedAt()
            );
        }).toList();

        return new PhotoCursorResponse(items, hasNext, nextCursor);
    }

    public PhotoDetailResponse getPhotoDetail(Long organizationId, Long userId, Long photoId) {
        validateActiveMember(organizationId, userId);

        Photo photo = photoRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(photoId, organizationId)
                .orElseThrow(() -> new GeneralException(PhotoErrorCode.PHOTO_NOT_FOUND));

        UploadedFile uploadedFile = uploadedFileRepository
                .findByIdAndOrganizationIdAndDeletedAtIsNull(photo.getUploadedFileId(), organizationId)
                .orElseThrow(() -> new GeneralException(PhotoErrorCode.FILE_NOT_FOUND));

        String memberName = membershipAccessFacade.getMembershipSummaryById(photo.getMembershipId())
                .map(MembershipSummary::memberName)
                .orElse("알 수 없음");

        FileInfo fileInfo = new FileInfo(
                uploadedFile.getId(),
                uploadedFile.getStorageKey(),
                uploadedFile.getOriginalName(),
                uploadedFile.getContentType(),
                uploadedFile.getSizeBytes(),
                fileStorageService.getFileUrl(uploadedFile.getStorageKey())
        );

        UploaderInfo uploaderInfo = new UploaderInfo(
                photo.getMembershipId(),
                memberName
        );

        return new PhotoDetailResponse(
                photo.getId(),
                photo.getOrganizationId(),
                photo.getTitle(),
                fileInfo,
                uploaderInfo,
                photo.getCreatedAt(),
                photo.getUpdatedAt()
        );
    }

    private void validateActiveMember(Long organizationId, Long userId) {
        if (!membershipAccessFacade.isActiveMember(organizationId, userId)) {
            throw new GeneralException(PhotoErrorCode.MEMBER_REQUIRED);
        }
    }
}
