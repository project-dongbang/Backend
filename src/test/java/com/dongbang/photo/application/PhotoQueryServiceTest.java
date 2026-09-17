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
import com.dongbang.photo.infrastructure.storage.FileStorageService;
import com.dongbang.photo.presentation.dto.response.PhotoCursorResponse;
import com.dongbang.photo.presentation.dto.response.PhotoDetailResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PhotoQueryServiceTest {

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private UploadedFileRepository uploadedFileRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private MembershipAccessFacade membershipAccessFacade;

    @InjectMocks
    private PhotoQueryService photoQueryService;

    private final Long orgId = 1L;
    private final Long userId = 10L;

    @Nested
    @DisplayName("커서 기반 무한 스크롤 목록 조회")
    class GetPhotosCursorTest {

        @Test
        @DisplayName("성공: 다음 페이지가 있는 경우 hasNext=true, nextCursor 반환")
        void getPhotosCursor_hasNextTrue() {
            // given: size=2 요청에 대해 3개의 결과 반환 (size + 1 패턴)
            int size = 2;
            Long cursor = null;

            Photo p1 = Photo.builder().id(30L).organizationId(orgId).uploadedFileId(1L).membershipId(100L).title("사진3").build();
            Photo p2 = Photo.builder().id(29L).organizationId(orgId).uploadedFileId(2L).membershipId(100L).title("사진2").build();
            Photo p3 = Photo.builder().id(28L).organizationId(orgId).uploadedFileId(3L).membershipId(100L).title("사진1").build();

            UploadedFile f1 = UploadedFile.builder().id(1L).organizationId(orgId).uploadedByMembershipId(100L).storageKey("k1").originalName("1.jpg").contentType("image/jpeg").sizeBytes(10L).build();
            UploadedFile f2 = UploadedFile.builder().id(2L).organizationId(orgId).uploadedByMembershipId(100L).storageKey("k2").originalName("2.jpg").contentType("image/jpeg").sizeBytes(10L).build();

            MembershipSummary member = new MembershipSummary(100L, orgId, userId, "김동방", MembershipRole.MEMBER, MembershipStatus.ACTIVE);

            given(membershipAccessFacade.isActiveMember(orgId, userId)).willReturn(true);
            given(photoRepository.findPhotosByCursor(eq(orgId), eq(cursor), any(Pageable.class)))
                    .willReturn(List.of(p1, p2, p3));
            given(uploadedFileRepository.findAllByIdInAndOrganizationIdAndDeletedAtIsNull(any(), eq(orgId)))
                    .willReturn(List.of(f1, f2));
            given(membershipAccessFacade.getMembershipSummariesByIds(any()))
                    .willReturn(Map.of(100L, member));
            given(fileStorageService.getFileUrl("k1")).willReturn("/uploads/k1");
            given(fileStorageService.getFileUrl("k2")).willReturn("/uploads/k2");

            // when
            PhotoCursorResponse response = photoQueryService.getPhotosCursor(orgId, userId, cursor, size);

            // then
            assertThat(response.content()).hasSize(2);
            assertThat(response.hasNext()).isTrue();
            assertThat(response.nextCursor()).isEqualTo(29L);
            assertThat(response.content().get(0).photoId()).isEqualTo(30L);
            assertThat(response.content().get(1).photoId()).isEqualTo(29L);
        }

        @Test
        @DisplayName("성공: 마지막 페이지인 경우 hasNext=false, nextCursor=null 반환")
        void getPhotosCursor_hasNextFalse() {
            // given: size=2 요청에 대해 1개의 결과만 반환
            int size = 2;
            Long cursor = 20L;

            Photo p = Photo.builder().id(15L).organizationId(orgId).uploadedFileId(1L).membershipId(100L).title("마지막 사진").build();
            UploadedFile f = UploadedFile.builder().id(1L).organizationId(orgId).uploadedByMembershipId(100L).storageKey("k1").originalName("1.jpg").contentType("image/jpeg").sizeBytes(10L).build();
            MembershipSummary member = new MembershipSummary(100L, orgId, userId, "김동방", MembershipRole.MEMBER, MembershipStatus.ACTIVE);

            given(membershipAccessFacade.isActiveMember(orgId, userId)).willReturn(true);
            given(photoRepository.findPhotosByCursor(eq(orgId), eq(cursor), any(Pageable.class)))
                    .willReturn(List.of(p));
            given(uploadedFileRepository.findAllByIdInAndOrganizationIdAndDeletedAtIsNull(any(), eq(orgId)))
                    .willReturn(List.of(f));
            given(membershipAccessFacade.getMembershipSummariesByIds(any()))
                    .willReturn(Map.of(100L, member));
            given(fileStorageService.getFileUrl("k1")).willReturn("/uploads/k1");

            // when
            PhotoCursorResponse response = photoQueryService.getPhotosCursor(orgId, userId, cursor, size);

            // then
            assertThat(response.content()).hasSize(1);
            assertThat(response.hasNext()).isFalse();
            assertThat(response.nextCursor()).isNull();
        }

        @Test
        @DisplayName("실패: 비동아리 회원이 목록 조회 시 MEMBER_REQUIRED 예외 발생")
        void getPhotosCursor_notMember_throwsException() {
            given(membershipAccessFacade.isActiveMember(orgId, userId)).willReturn(false);

            assertThatThrownBy(() -> photoQueryService.getPhotosCursor(orgId, userId, null, 20))
                    .isInstanceOf(GeneralException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PhotoErrorCode.MEMBER_REQUIRED);
        }
    }

    @Nested
    @DisplayName("사진 상세 조회")
    class GetPhotoDetailTest {

        @Test
        @DisplayName("성공: 동아리 회원이 사진 상세 정보를 조회한다")
        void getPhotoDetail_success() {
            // given
            Long photoId = 50L;
            Photo photo = Photo.builder().id(photoId).organizationId(orgId).uploadedFileId(1L).membershipId(100L).title("상세 사진").build();
            UploadedFile file = UploadedFile.builder().id(1L).organizationId(orgId).uploadedByMembershipId(100L).storageKey("key").originalName("mt.jpg").contentType("image/jpeg").sizeBytes(500L).build();
            MembershipSummary summary = new MembershipSummary(100L, orgId, userId, "홍길동", MembershipRole.MEMBER, MembershipStatus.ACTIVE);

            given(membershipAccessFacade.isActiveMember(orgId, userId)).willReturn(true);
            given(photoRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(photoId, orgId)).willReturn(Optional.of(photo));
            given(uploadedFileRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(1L, orgId)).willReturn(Optional.of(file));
            given(membershipAccessFacade.getMembershipSummaryById(100L)).willReturn(Optional.of(summary));
            given(fileStorageService.getFileUrl("key")).willReturn("/uploads/key");

            // when
            PhotoDetailResponse response = photoQueryService.getPhotoDetail(orgId, userId, photoId);

            // then
            assertThat(response.photoId()).isEqualTo(photoId);
            assertThat(response.title()).isEqualTo("상세 사진");
            assertThat(response.uploader().memberName()).isEqualTo("홍길동");
            assertThat(response.file().storageKey()).isEqualTo("key");
        }

        @Test
        @DisplayName("실패: 삭제되었거나 존재하지 않는 사진 조회 시 PHOTO_NOT_FOUND 예외 발생")
        void getPhotoDetail_notFound_throwsException() {
            Long photoId = 999L;
            given(membershipAccessFacade.isActiveMember(orgId, userId)).willReturn(true);
            given(photoRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(photoId, orgId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> photoQueryService.getPhotoDetail(orgId, userId, photoId))
                    .isInstanceOf(GeneralException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PhotoErrorCode.PHOTO_NOT_FOUND);
        }
    }
}
