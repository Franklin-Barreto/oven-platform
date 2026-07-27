package br.com.f2e.ovenplatform.media.domain;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotBlank;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import br.com.f2e.ovenplatform.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "stored_images")
public class StoredImage extends BaseEntity {

  private static final int MAX_OBJECT_KEY_LENGTH = 512;
  private static final int MAX_CHECKSUM_LENGTH = 128;

  private static final Set<String> SUPPORTED_CONTENT_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp");

  @Column(nullable = false)
  private UUID tenantId;

  @Column(nullable = false, unique = true, length = MAX_OBJECT_KEY_LENGTH)
  private String objectKey;

  @Column(nullable = false, length = 100)
  private String contentType;

  @Column(nullable = false)
  private long sizeBytes;

  @Column(nullable = false, length = MAX_CHECKSUM_LENGTH)
  private String checksum;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private StoredImageStatus status;

  protected StoredImage() {}

  private StoredImage(
      UUID tenantId, String objectKey, String contentType, long sizeBytes, String checksum) {

    this.tenantId = tenantId;
    this.objectKey = objectKey;
    this.contentType = contentType;
    this.sizeBytes = sizeBytes;
    this.checksum = checksum;
    this.status = StoredImageStatus.PENDING;
  }

  public static StoredImage pending(
      UUID tenantId, String objectKey, String contentType, long sizeBytes, String checksum) {

    return new StoredImage(
        requireNotNull(tenantId, "tenantId"),
        requireObjectKey(objectKey),
        requireContentType(contentType),
        requirePositiveSize(sizeBytes),
        requireChecksum(checksum));
  }

  public boolean isAvailable() {
    return status == StoredImageStatus.AVAILABLE;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public String getObjectKey() {
    return objectKey;
  }

  public String getContentType() {
    return contentType;
  }

  public long getSizeBytes() {
    return sizeBytes;
  }

  public String getChecksum() {
    return checksum;
  }

  public StoredImageStatus getStatus() {
    return status;
  }

  public void confirm(String actualContentType, long actualSizeBytes, String actualChecksum) {

    if (isAvailable()) {
      return;
    }

    var metadataMatches =
        contentType.equals(actualContentType)
            && sizeBytes == actualSizeBytes
            && Objects.equals(checksum, actualChecksum);

    if (!metadataMatches) {
      throw new StoredImageMetadataMismatchException();
    }

    status = StoredImageStatus.AVAILABLE;
  }

  private static String requireObjectKey(String objectKey) {
    var normalized = requireNotBlank(objectKey, "objectKey");

    if (normalized.length() > MAX_OBJECT_KEY_LENGTH) {
      throw new IllegalArgumentException(
          "objectKey must have at most %d characters".formatted(MAX_OBJECT_KEY_LENGTH));
    }

    return normalized;
  }

  private static String requireContentType(String contentType) {
    var normalized = requireNotBlank(contentType, "contentType").toLowerCase(Locale.ROOT);

    if (!SUPPORTED_CONTENT_TYPES.contains(normalized)) {
      throw new IllegalArgumentException("Unsupported image content type: " + normalized);
    }

    return normalized;
  }

  private static long requirePositiveSize(long sizeBytes) {
    if (sizeBytes <= 0) {
      throw new IllegalArgumentException("sizeBytes must be greater than zero");
    }

    return sizeBytes;
  }

  private static String requireChecksum(String checksum) {
    var normalized = requireNotBlank(checksum, "checksum");

    if (normalized.length() > MAX_CHECKSUM_LENGTH) {
      throw new IllegalArgumentException(
          "checksum must have at most %d characters".formatted(MAX_CHECKSUM_LENGTH));
    }

    return normalized;
  }
}
