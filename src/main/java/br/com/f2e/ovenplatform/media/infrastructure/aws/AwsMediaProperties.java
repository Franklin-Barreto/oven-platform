package br.com.f2e.ovenplatform.media.infrastructure.aws;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "oven.media.aws")
public record AwsMediaProperties(
    @NotBlank String bucket, @NotBlank String region, @NotNull Duration uploadAuthorizationTtl) {}
