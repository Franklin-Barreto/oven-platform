package br.com.f2e.ovenplatform.media.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class PrepareImageUploadCommandTest {

  @Test
  void shouldNormalizeContentTypeAndChecksum() {
    var command = new PrepareImageUploadCommand(" IMAGE/WEBP ", 1024, " checksum ");

    assertThat(command.contentType()).isEqualTo("image/webp");
    assertThat(command.checksum()).isEqualTo("checksum");
  }

  @Test
  void shouldRejectNonPositiveSize() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new PrepareImageUploadCommand("image/webp", 0, "checksum"))
        .withMessage("sizeBytes must be greater than zero");
  }
}
