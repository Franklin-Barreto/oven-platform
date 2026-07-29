package br.com.f2e.ovenplatform.media.infrastructure.aws;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotBlank;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oven.media.aws")
public record AwsMediaProperties(String bucket, String region, Duration uploadAuthorizationTtl) {

  public AwsMediaProperties {
    bucket = requireNotBlank(bucket, "bucket");
    region = requireNotBlank(region, "region");

    if (uploadAuthorizationTtl == null
        || uploadAuthorizationTtl.isZero()
        || uploadAuthorizationTtl.isNegative()) {
      throw new IllegalArgumentException("uploadAuthorizationTtl must be positive");
    }
  }
}
