package com.dongbang.photo.infrastructure.storage.s3;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "app.aws.s3")
@ConditionalOnProperty(prefix = "app.aws.s3", name = "enabled", havingValue = "true")
@Validated
@Getter
@Setter
public class S3Properties {

    private boolean enabled = false;

    @NotBlank
    private String bucket;

    @NotBlank
    private String region = "ap-northeast-2";
    private String endpoint;
    private String customDomain;
    private Duration urlTtl = Duration.ofMinutes(15);
}
