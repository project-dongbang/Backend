package com.dongbang.photo.infrastructure.storage.s3;

import com.dongbang.global.exception.GeneralException;
import com.dongbang.photo.exception.PhotoErrorCode;
import com.dongbang.photo.infrastructure.storage.FileStorageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3FileStorageServiceTest {

    private static final byte[] PNG_BYTES = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x01
    };
    private static final byte[] JPEG_BYTES = new byte[] {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01
    };

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private S3Properties s3Properties;

    @InjectMocks
    private S3FileStorageService s3FileStorageService;

    @BeforeEach
    void setUp() {
        // Default lenient properties if needed
    }

    @Test
    @DisplayName("성공: 유효한 이미지 파일이 S3에 업로드되고 결과가 반환된다")
    void store_success() {
        // given
        given(s3Properties.getBucket()).willReturn("dongbang-storage");
        MockMultipartFile file = new MockMultipartFile(
                "file", "gallery.png", "image/png", PNG_BYTES
        );

        // when
        FileStorageResult result = s3FileStorageService.store(file, 10L);

        // then
        assertThat(result.originalName()).isEqualTo("gallery.png");
        assertThat(result.contentType()).isEqualTo("image/png");
        assertThat(result.storageKey()).startsWith("organizations/10/photos/");
        assertThat(result.storageKey()).endsWith(".png");
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("실패: 빈 파일 업로드 시 FILE_EMPTY 예외 발생")
    void store_emptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> s3FileStorageService.store(emptyFile, 10L))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("errorCode", PhotoErrorCode.FILE_EMPTY);
    }

    @Test
    @DisplayName("실패: 이미지가 아닌 파일 업로드 시 INVALID_FILE_TYPE 예외 발생")
    void store_invalidContentType() {
        MockMultipartFile textFile = new MockMultipartFile("file", "doc.pdf", "application/pdf", "pdf-data".getBytes());

        assertThatThrownBy(() -> s3FileStorageService.store(textFile, 10L))
                .isInstanceOf(GeneralException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PhotoErrorCode.INVALID_FILE_TYPE);
    }

    @Test
    @DisplayName("실패: 이미지 MIME으로 위장한 파일은 INVALID_FILE_TYPE 예외 발생")
    void store_spoofedImageContentType() {
        MockMultipartFile spoofedFile = new MockMultipartFile(
                "file", "fake.jpg", "image/jpeg", "not-an-image".getBytes()
        );

        assertThatThrownBy(() -> s3FileStorageService.store(spoofedFile, 10L))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("errorCode", PhotoErrorCode.INVALID_FILE_TYPE);
    }

    @Test
    @DisplayName("실패: S3 업로드 중 에러 발생 시 FILE_UPLOAD_FAILED 예외 발생")
    void store_s3Error() {
        // given
        given(s3Properties.getBucket()).willReturn("dongbang-storage");
        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .willThrow(S3Exception.builder().message("S3 connection error").build());

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", JPEG_BYTES
        );

        // when & then
        assertThatThrownBy(() -> s3FileStorageService.store(file, 10L))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("errorCode", PhotoErrorCode.FILE_UPLOAD_FAILED);
    }

    @Test
    @DisplayName("성공: 비공개 S3 객체의 presigned URL 생성")
    void getFileUrl_default() throws Exception {
        // given
        given(s3Properties.getBucket()).willReturn("dongbang-storage");
        given(s3Properties.getCustomDomain()).willReturn(null);
        given(s3Properties.getUrlTtl()).willReturn(Duration.ofMinutes(15));
        PresignedGetObjectRequest presignedRequest = org.mockito.Mockito.mock(PresignedGetObjectRequest.class);
        given(presignedRequest.url()).willReturn(URI.create("https://signed.example.com/sample.jpg?signature=test").toURL());
        given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).willReturn(presignedRequest);

        // when
        String url = s3FileStorageService.getFileUrl("organizations/1/photos/sample.jpg");

        // then
        assertThat(url).isEqualTo("https://signed.example.com/sample.jpg?signature=test");
    }

    @Test
    @DisplayName("성공: 커스텀 도메인(CDN) S3 URL 생성")
    void getFileUrl_customDomain() {
        // given
        given(s3Properties.getCustomDomain()).willReturn("https://cdn.dongbang.com");

        // when
        String url = s3FileStorageService.getFileUrl("organizations/1/photos/sample.jpg");

        // then
        assertThat(url).isEqualTo("https://cdn.dongbang.com/organizations/1/photos/sample.jpg");
    }

    @Test
    @DisplayName("성공: 파일 삭제 시 S3 deleteObject 호출")
    void delete_success() {
        // given
        given(s3Properties.getBucket()).willReturn("dongbang-storage");

        // when
        s3FileStorageService.delete("organizations/1/photos/sample.jpg");

        // then
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }
}
