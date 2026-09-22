package com.dongbang.receipt.infrastructure;

import com.dongbang.global.exception.GeneralException;
import com.dongbang.receipt.presentation.ReceiptOcrResponse;
import com.dongbang.receipt.exception.ReceiptErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Component
public class ReceiptOcrClient {
    private final RestClient restClient;

    public ReceiptOcrClient(RestClient.Builder builder,
                            @Value("${app.receipt-ocr.url:http://ocr:8000}") String url) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(90));

        this.restClient = builder
                .requestFactory(requestFactory)
                .baseUrl(url)
                .build();
    }

    public ReceiptOcrResponse recognize(MultipartFile file) {
        try {
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() == null ? "receipt.jpg" : file.getOriginalFilename();
                }
            };
            HttpHeaders partHeaders = new HttpHeaders();
            partHeaders.setContentType(MediaType.parseMediaType(
                    file.getContentType() == null ? MediaType.IMAGE_JPEG_VALUE : file.getContentType()));
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new HttpEntity<>(resource, partHeaders));
            ReceiptOcrResponse response = restClient.post()
                    .uri("/ocr")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(ReceiptOcrResponse.class);
            if (response == null) {
                throw new GeneralException(ReceiptErrorCode.OCR_FAILED);
            }
            return response;
        } catch (IOException | RuntimeException ex) {
            log.error("Failed to recognize receipt image through OCR service: {}", ex.getMessage(), ex);
            if (ex instanceof GeneralException ge) {
                throw ge;
            }
            throw new GeneralException(ReceiptErrorCode.OCR_FAILED);
        }
    }
}
