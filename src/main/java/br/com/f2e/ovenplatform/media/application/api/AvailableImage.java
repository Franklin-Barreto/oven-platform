package br.com.f2e.ovenplatform.media.application.api;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import java.net.URI;
import java.util.UUID;

public record AvailableImage(UUID id, URI publicUrl) {

  public AvailableImage {
    requireNotNull(id, "id");
    requireNotNull(publicUrl, "publicUrl");
  }
}
