package com.dongbang.photo.application;

import com.dongbang.global.exception.GeneralException;
import com.dongbang.global.response.code.GeneralErrorCode;
import com.dongbang.organization.application.facade.MembershipAccessFacade;
import com.dongbang.organization.application.facade.MembershipSummary;
import com.dongbang.photo.domain.Photo;
import com.dongbang.photo.domain.UploadedFile;
import com.dongbang.photo.domain.repository.PhotoRepository;
import com.dongbang.photo.domain.repository.UploadedFileRepository;
import com.dongbang.photo.exception.PhotoErrorCode;
import com.dongbang.photo.infrastructure.storage.FileStorageResult;
import com.dongbang.photo.infrastructure.storage.FileStorageService;
import com.dongbang.photo.presentation.dto.request.UpdatePhotoRequest;
import com.dongbang.photo.presentation.dto.response.FileInfo;
import com.dongbang.photo.presentation.dto.response.PhotoDetailResponse;
import com.dongbang.photo.presentation.dto.response.UploaderInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class PhotoCommandService {

    private final PhotoRepository photoRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final FileStorageService fileStorageService;
    private final MembershipAccessFacade membershipAccessFacade;

    public PhotoDetailResponse createPhoto(Long organizationId, Long userId, MultipartFile file, String title) {
        validateStaff(organizationId, userId);

        MembershipSummary uploader = membershipAccessFacade.getMembershipSummary(organizationId, userId)
                .orElseThrow(() -> new GeneralException(PhotoErrorCode.MEMBER_REQUIRED));

        FileStorageResult storageResult = fileStorageService.store(file, organizationId);
        try {
            UploadedFile uploadedFile = uploadedFileRepository.save(UploadedFile.builder()
                    .organizationId(organizationId)
                    .uploadedByMembershipId(uploader.membershipId())
                    .storageKey(storageResult.storageKey())
                    .originalName(storageResult.originalName())
                    .contentType(storageResult.contentType())
                    .sizeBytes(storageResult.sizeBytes())
                    .checksum(storageResult.checksum())
                    .build());

            Photo photo = photoRepository.save(Photo.builder()
                    .organizationId(organizationId)
                    .uploadedFileId(uploadedFile.getId())
                    .membershipId(uploader.membershipId())
                    .title(title)
                    .build());

            return toDetailResponse(photo, uploadedFile, uploader.memberName());
        } catch (RuntimeException ex) {
            fileStorageService.delete(storageResult.storageKey());
            throw ex;
        }
    }

    public PhotoDetailResponse createPhotoWithFileId(Long organizationId, Long userId, Long uploadedFileId, String title) {
        validateStaff(organizationId, userId);

        MembershipSummary uploader = membershipAccessFacade.getMembershipSummary(organizationId, userId)
                .orElseThrow(() -> new GeneralException(PhotoErrorCode.MEMBER_REQUIRED));

        UploadedFile uploadedFile = uploadedFileRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(uploadedFileId, organizationId)
                .orElseThrow(() -> new GeneralException(PhotoErrorCode.FILE_NOT_FOUND));

        Photo photo = Photo.builder()
                .organizationId(organizationId)
                .uploadedFileId(uploadedFile.getId())
                .membershipId(uploader.membershipId())
                .title(title)
                .build();
        photo = photoRepository.save(photo);

        return toDetailResponse(photo, uploadedFile, uploader.memberName());
    }

    public PhotoDetailResponse updatePhoto(Long organizationId, Long userId, Long photoId, UpdatePhotoRequest request) {
        validateStaff(organizationId, userId);

        Photo photo = photoRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(photoId, organizationId)
                .orElseThrow(() -> new GeneralException(PhotoErrorCode.PHOTO_NOT_FOUND));

        photo.updateTitle(request.title());

        UploadedFile uploadedFile = uploadedFileRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(photo.getUploadedFileId(), organizationId)
                .orElseThrow(() -> new GeneralException(PhotoErrorCode.FILE_NOT_FOUND));

        String memberName = membershipAccessFacade.getMembershipSummaryById(photo.getMembershipId())
                .map(MembershipSummary::memberName)
                .orElse("알 수 없음");

        return toDetailResponse(photo, uploadedFile, memberName);
    }

    public PhotoDetailResponse replacePhotoImage(Long organizationId, Long userId, Long photoId,
                                                 MultipartFile file, String title) {
        validateStaff(organizationId, userId);
        if (title != null && title.length() > 200) {
            throw new GeneralException(GeneralErrorCode.VALIDATION_ERROR);
        }

        Photo photo = photoRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(photoId, organizationId)
                .orElseThrow(() -> new GeneralException(PhotoErrorCode.PHOTO_NOT_FOUND));
        UploadedFile previousFile = uploadedFileRepository
                .findByIdAndOrganizationIdAndDeletedAtIsNull(photo.getUploadedFileId(), organizationId)
                .orElseThrow(() -> new GeneralException(PhotoErrorCode.FILE_NOT_FOUND));
        MembershipSummary replacer = membershipAccessFacade.getMembershipSummary(organizationId, userId)
                .orElseThrow(() -> new GeneralException(PhotoErrorCode.MEMBER_REQUIRED));

        FileStorageResult stored = fileStorageService.store(file, organizationId);
        try {
            UploadedFile replacement = uploadedFileRepository.save(UploadedFile.builder()
                    .organizationId(organizationId)
                    .uploadedByMembershipId(replacer.membershipId())
                    .storageKey(stored.storageKey())
                    .originalName(stored.originalName())
                    .contentType(stored.contentType())
                    .sizeBytes(stored.sizeBytes())
                    .checksum(stored.checksum())
                    .build());

            photo.replaceUploadedFile(replacement.getId());
            if (title != null) {
                photo.updateTitle(title);
            }

            // JSON 등록은 하나의 업로드 파일을 여러 사진에 연결할 수 있으므로 참조 중인 파일은 보존한다.
            boolean deletePrevious = isUnusedPhotoFile(organizationId, previousFile);
            if (deletePrevious) {
                previousFile.delete();
            }

            String memberName = membershipAccessFacade.getMembershipSummaryById(photo.getMembershipId())
                    .map(MembershipSummary::memberName)
                    .orElse("알 수 없음");
            PhotoDetailResponse response = toDetailResponse(photo, replacement, memberName);
            cleanupReplacedFiles(stored.storageKey(), deletePrevious ? previousFile.getStorageKey() : null);
            return response;
        } catch (RuntimeException | Error ex) {
            fileStorageService.delete(stored.storageKey());
            throw ex;
        }
    }

    public void deletePhoto(Long organizationId, Long userId, Long photoId) {
        validateStaff(organizationId, userId);

        Photo photo = photoRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(photoId, organizationId)
                .orElseThrow(() -> new GeneralException(PhotoErrorCode.PHOTO_NOT_FOUND));

        UploadedFile uploadedFile = uploadedFileRepository
                .findByIdAndOrganizationIdAndDeletedAtIsNull(photo.getUploadedFileId(), organizationId)
                .orElseThrow(() -> new GeneralException(PhotoErrorCode.FILE_NOT_FOUND));
        photo.delete();
        if (isUnusedPhotoFile(organizationId, uploadedFile)) {
            uploadedFile.delete();
            deleteAfterCommit(uploadedFile.getStorageKey());
        }
    }

    private void validateStaff(Long organizationId, Long userId) {
        if (!membershipAccessFacade.isStaff(organizationId, userId)) {
            throw new GeneralException(PhotoErrorCode.STAFF_REQUIRED);
        }
    }

    private boolean isUnusedPhotoFile(Long organizationId, UploadedFile uploadedFile) {
        return uploadedFile.getStorageKey().startsWith("organizations/" + organizationId + "/photos/")
                && photoRepository.countByUploadedFileIdAndDeletedAtIsNull(uploadedFile.getId()) == 0;
    }

    private void deleteAfterCommit(String storageKey) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            fileStorageService.delete(storageKey);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                fileStorageService.delete(storageKey);
            }
        });
    }

    private void cleanupReplacedFiles(String replacementKey, String previousKey) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            if (previousKey != null) {
                fileStorageService.delete(previousKey);
            }
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (previousKey != null) {
                    fileStorageService.delete(previousKey);
                }
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    fileStorageService.delete(replacementKey);
                }
            }
        });
    }

    private PhotoDetailResponse toDetailResponse(Photo photo, UploadedFile uploadedFile, String memberName) {
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
}
