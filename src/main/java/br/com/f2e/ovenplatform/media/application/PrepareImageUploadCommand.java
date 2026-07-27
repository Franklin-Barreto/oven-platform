package br.com.f2e.ovenplatform.media.application;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotBlank;

import java.util.Locale;

public record PrepareImageUploadCommand(String contentType, long sizeBytes, String checksum) {

  public PrepareImageUploadCommand {
    contentType = requireNotBlank(contentType, "contentType").toLowerCase(Locale.ROOT);
    checksum = requireNotBlank(checksum, "checksum");

    if (sizeBytes <= 0) {
      throw new IllegalArgumentException("sizeBytes must be greater than zero");
    }
  }
}
