package br.com.f2e.ovenplatform.media.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class StoredImageTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final String OBJECT_KEY =
      "tenants/%s/images/%s.webp".formatted(TENANT_ID, UUID.randomUUID());

  @Test
  void shouldCreatePendingImage() {
    var image = StoredImage.pending(TENANT_ID, OBJECT_KEY, "image/webp", 1024, "sha256-checksum");

    assertThat(image.getTenantId()).isEqualTo(TENANT_ID);
    assertThat(image.getObjectKey()).isEqualTo(OBJECT_KEY);
    assertThat(image.getContentType()).isEqualTo("image/webp");
    assertThat(image.getSizeBytes()).isEqualTo(1024);
    assertThat(image.getChecksum()).isEqualTo("sha256-checksum");
    assertThat(image.getStatus()).isEqualTo(StoredImageStatus.PENDING);
    assertThat(image.isAvailable()).isFalse();
  }

  @Test
  void shouldConfirmImageWhenMetadataMatches() {
    var image = StoredImage.pending(TENANT_ID, OBJECT_KEY, "image/png", 2048, "checksum");

    image.confirm("image/png", 2048, "checksum");
    image.confirm("image/png", 2048, "checksum");

    assertThat(image.getStatus()).isEqualTo(StoredImageStatus.AVAILABLE);
    assertThat(image.isAvailable()).isTrue();
  }

  @Test
  void shouldRejectConfirmationWhenMetadataDoesNotMatch() {
    var image = StoredImage.pending(TENANT_ID, OBJECT_KEY, "image/png", 2048, "checksum");

    assertThatThrownBy(() -> image.confirm("image/png", 2049, "checksum"))
        .isInstanceOf(StoredImageMetadataMismatchException.class)
        .hasMessage("Stored image metadata does not match the expected upload metadata");

    assertThat(image.isAvailable()).isFalse();
  }

  @Test
  void shouldNormalizeContentTypeAndChecksum() {
    var image = StoredImage.pending(TENANT_ID, OBJECT_KEY, "IMAGE/JPEG", 2048, "  checksum  ");

    assertThat(image.getContentType()).isEqualTo("image/jpeg");
    assertThat(image.getChecksum()).isEqualTo("checksum");
  }

  @Test
  void shouldRejectUnsupportedContentType() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> StoredImage.pending(TENANT_ID, OBJECT_KEY, "image/svg+xml", 1024, "checksum"))
        .withMessage("Unsupported image content type: image/svg+xml");
  }

  @Test
  void shouldRejectNonPositiveSize() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> StoredImage.pending(TENANT_ID, OBJECT_KEY, "image/webp", 0, "checksum"))
        .withMessage("sizeBytes must be greater than zero");
  }

  @Test
  void shouldRejectBlankObjectKey() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> StoredImage.pending(TENANT_ID, " ", "image/webp", 1024, "checksum"))
        .withMessage("objectKey must not be blank");
  }

  @Test
  void shouldRejectBlankChecksum() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> StoredImage.pending(TENANT_ID, OBJECT_KEY, "image/webp", 1024, " "))
        .withMessage("checksum must not be blank");
  }
}
