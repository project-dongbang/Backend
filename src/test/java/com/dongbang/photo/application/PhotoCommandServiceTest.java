package com.dongbang.photo.application;

import com.dongbang.global.exception.GeneralException;
import com.dongbang.organization.application.facade.MembershipAccessFacade;
import com.dongbang.organization.application.facade.MembershipSummary;
import com.dongbang.organization.domain.MembershipRole;
import com.dongbang.organization.domain.MembershipStatus;
import com.dongbang.photo.domain.Photo;
import com.dongbang.photo.domain.UploadedFile;
import com.dongbang.photo.domain.repository.PhotoRepository;
import com.dongbang.photo.domain.repository.UploadedFileRepository;
import com.dongbang.photo.exception.PhotoErrorCode;
import com.dongbang.photo.infrastructure.storage.FileStorageResult;
import com.dongbang.photo.infrastructure.storage.FileStorageService;
import com.dongbang.photo.presentation.dto.request.UpdatePhotoRequest;
import com.dongbang.photo.presentation.dto.response.PhotoDetailResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PhotoCommandServiceTest {

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private UploadedFileRepository uploadedFileRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private MembershipAccessFacade membershipAccessFacade;

    @InjectMocks
    private PhotoCommandService photoCommandService;

    private final Long orgId = 1L;
    private final Long userId = 10L;
    private final Long membershipId = 100L;

    @Nested
    @DisplayName("사진 등록")
    class CreatePhotoTest {

        @Test
        @DisplayName("성공: 운영진이 파일을 업로드하여 사진을 등록한다")
        void createPhoto_success() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "mt.jpg", "image/jpeg", "image-content".getBytes()
            );
            MembershipSummary summary = new MembershipSummary(
                    membershipId, orgId, userId, "운영진홍길동", MembershipRole.ADMIN, MembershipStatus.ACTIVE
            );
            FileStorageResult storageResult = new FileStorageResult(
                    "organizations/1/photos/uuid_mt.jpg", "mt.jpg", "image/jpeg", 1234L, "checksum"
            );
            UploadedFile savedFile = UploadedFile.builder()
                    .id(1L)
                    .organizationId(orgId)
                    .uploadedByMembershipId(membershipId)
                    .storageKey(storageResult.storageKey())
                    .originalName(storageResult.originalName())
                    .contentType(storageResult.contentType())
                    .sizeBytes(storageResult.sizeBytes())
                    .checksum(storageResult.checksum())
                    .build();
            Photo savedPhoto = Photo.builder()
                    .id(50L)
                    .organizationId(orgId)
                    .uploadedFileId(1L)
                    .membershipId(membershipId)
                    .title("MT 단체사진")
                    .build();

            given(membershipAccessFacade.isStaff(orgId, userId)).willReturn(true);
            given(membershipAccessFacade.getMembershipSummary(orgId, userId)).willReturn(Optional.of(summary));
            given(fileStorageService.store(file, orgId)).willReturn(storageResult);
            given(uploadedFileRepository.save(any(UploadedFile.class))).willReturn(savedFile);
            given(photoRepository.save(any(Photo.class))).willReturn(savedPhoto);
            given(fileStorageService.getFileUrl(savedFile.getStorageKey())).willReturn("/uploads/" + savedFile.getStorageKey());

            // when
            PhotoDetailResponse response = photoCommandService.createPhoto(orgId, userId, file, "MT 단체사진");

            // then
            assertThat(response).isNotNull();
            assertThat(response.photoId()).isEqualTo(50L);
            assertThat(response.title()).isEqualTo("MT 단체사진");
            assertThat(response.file().storageKey()).isEqualTo(storageResult.storageKey());
            assertThat(response.uploader().memberName()).isEqualTo("운영진홍길동");
            verify(photoRepository).save(any(Photo.class));
            verify(uploadedFileRepository).save(any(UploadedFile.class));
        }

        @Test
        @DisplayName("실패: 일반 회원이 사진 등록 시도 시 STAFF_REQUIRED 예외 발생")
        void createPhoto_notStaff_throwsException() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "mt.jpg", "image/jpeg", "image-content".getBytes()
            );
            given(membershipAccessFacade.isStaff(orgId, userId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> photoCommandService.createPhoto(orgId, userId, file, "제목"))
                    .isInstanceOf(GeneralException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PhotoErrorCode.STAFF_REQUIRED);
        }
    }

    @Nested
    @DisplayName("사진 정보 수정")
    class UpdatePhotoTest {

        @Test
        @DisplayName("성공: 운영진이 사진 제목을 수정한다")
        void updatePhoto_success() {
            // given
            Long photoId = 50L;
            UpdatePhotoRequest request = new UpdatePhotoRequest("수정된 제목");
            Photo photo = Photo.builder()
                    .id(photoId)
                    .organizationId(orgId)
                    .uploadedFileId(1L)
                    .membershipId(membershipId)
                    .title("이전 제목")
                    .build();
            UploadedFile file = UploadedFile.builder()
                    .id(1L)
                    .organizationId(orgId)
                    .uploadedByMembershipId(membershipId)
                    .storageKey("key")
                    .originalName("mt.jpg")
                    .contentType("image/jpeg")
                    .sizeBytes(100L)
                    .build();
            MembershipSummary summary = new MembershipSummary(
                    membershipId, orgId, userId, "운영진홍길동", MembershipRole.ADMIN, MembershipStatus.ACTIVE
            );

            given(membershipAccessFacade.isStaff(orgId, userId)).willReturn(true);
            given(photoRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(photoId, orgId)).willReturn(Optional.of(photo));
            given(uploadedFileRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(1L, orgId)).willReturn(Optional.of(file));
            given(membershipAccessFacade.getMembershipSummaryById(membershipId)).willReturn(Optional.of(summary));
            given(fileStorageService.getFileUrl("key")).willReturn("/uploads/key");

            // when
            PhotoDetailResponse response = photoCommandService.updatePhoto(orgId, userId, photoId, request);

            // then
            assertThat(response.title()).isEqualTo("수정된 제목");
            assertThat(photo.getTitle()).isEqualTo("수정된 제목");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사진 수정 시 PHOTO_NOT_FOUND 예외 발생")
        void updatePhoto_notFound_throwsException() {
            // given
            Long photoId = 999L;
            UpdatePhotoRequest request = new UpdatePhotoRequest("수정된 제목");
            given(membershipAccessFacade.isStaff(orgId, userId)).willReturn(true);
            given(photoRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(photoId, orgId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> photoCommandService.updatePhoto(orgId, userId, photoId, request))
                    .isInstanceOf(GeneralException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PhotoErrorCode.PHOTO_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("사진 삭제")
    class DeletePhotoTest {

        @Test
        @DisplayName("성공: 운영진이 사진을 소프트 삭제한다")
        void deletePhoto_success() {
            // given
            Long photoId = 50L;
            Photo photo = Photo.builder()
                    .id(photoId)
                    .organizationId(orgId)
                    .uploadedFileId(1L)
                    .membershipId(membershipId)
                    .title("삭제될 사진")
                    .build();

            given(membershipAccessFacade.isStaff(orgId, userId)).willReturn(true);
            given(photoRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(photoId, orgId)).willReturn(Optional.of(photo));
            UploadedFile uploadedFile = UploadedFile.builder()
                    .id(1L)
                    .organizationId(orgId)
                    .uploadedByMembershipId(membershipId)
                    .storageKey("organizations/1/photos/deleted.jpg")
                    .originalName("deleted.jpg")
                    .contentType("image/jpeg")
                    .sizeBytes(100L)
                    .build();
            given(uploadedFileRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(1L, orgId))
                    .willReturn(Optional.of(uploadedFile));

            // when
            photoCommandService.deletePhoto(orgId, userId, photoId);

            // then
            assertThat(photo.isDeleted()).isTrue();
            assertThat(photo.getDeletedAt()).isNotNull();
            assertThat(uploadedFile.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("실패: 일반 회원이 삭제 시도 시 STAFF_REQUIRED 예외 발생")
        void deletePhoto_notStaff_throwsException() {
            // given
            Long photoId = 50L;
            given(membershipAccessFacade.isStaff(orgId, userId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> photoCommandService.deletePhoto(orgId, userId, photoId))
                    .isInstanceOf(GeneralException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PhotoErrorCode.STAFF_REQUIRED);
        }
    }
}
