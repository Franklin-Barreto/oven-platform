package br.com.f2e.ovenplatform.media.application.storage;

import static br.com.f2e.ovenplatform.media.domain.validation.MediaPreconditions.requireSha256Checksum;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotBlank;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requirePositive;

public record StoredObjectMetadata(String contentType, long sizeBytes, String checksum) {

  public StoredObjectMetadata {
    contentType = requireNotBlank(contentType, "contentType");
    requirePositive(sizeBytes, "sizeBytes");
    checksum = requireSha256Checksum(checksum);
  }
}
