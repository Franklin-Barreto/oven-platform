package br.com.f2e.ovenplatform.media.application;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotBlank;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requirePositive;

import java.util.Locale;

public record PrepareImageUploadCommand(String contentType, long sizeBytes, String checksum) {

  public PrepareImageUploadCommand {
    contentType = requireNotBlank(contentType, "contentType").toLowerCase(Locale.ROOT);
    requirePositive(sizeBytes, "sizeBytes");
    checksum = requireNotBlank(checksum, "checksum");
  }
}
