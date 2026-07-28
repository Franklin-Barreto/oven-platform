package br.com.f2e.ovenplatform.media.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class PrepareImageUploadCommandTest {

  private static final String CHECKSUM = "0t/CUcGnJF1Ot9leX4FUcsbbz37maQu9fBkS9He2wio=";

  @Test
  void shouldNormalizeContentTypeAndChecksum() {
    var command = new PrepareImageUploadCommand(" IMAGE/WEBP ", 1024, " " + CHECKSUM + " ");

    assertThat(command.contentType()).isEqualTo("image/webp");
    assertThat(command.checksum()).isEqualTo(CHECKSUM);
  }

  @Test
  void shouldRejectNonPositiveSize() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new PrepareImageUploadCommand("image/webp", 0, CHECKSUM))
        .withMessage("sizeBytes must be greater than zero");
  }

  @Test
  void shouldRejectChecksumThatIsNotBase64EncodedSha256() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new PrepareImageUploadCommand("image/webp", 1024, "invalid"))
        .withMessage("checksum must be a Base64-encoded SHA-256 digest");
  }
}
