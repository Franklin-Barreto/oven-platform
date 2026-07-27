package br.com.f2e.ovenplatform.media.application.storage;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotBlank;

public record UploadAuthorizationSpec(
    String objectKey, String contentType, long sizeBytes, String checksum) {

  public UploadAuthorizationSpec {
    objectKey = requireNotBlank(objectKey, "objectKey");
    contentType = requireNotBlank(contentType, "contentType");
    checksum = requireNotBlank(checksum, "checksum");

    if (sizeBytes <= 0) {
      throw new IllegalArgumentException("sizeBytes must be greater than zero");
    }
  }
}
