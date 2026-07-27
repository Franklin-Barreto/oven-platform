package br.com.f2e.ovenplatform.media.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "oven.media")
public record MediaProperties(DataSize maxUploadSize) {

  public MediaProperties {
    if (maxUploadSize == null || maxUploadSize.toBytes() <= 0) {
      throw new IllegalArgumentException("oven.media.max-upload-size must be greater than zero");
    }
  }

  public long maxUploadSizeBytes() {
    return maxUploadSize.toBytes();
  }
}
