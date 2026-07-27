package br.com.f2e.ovenplatform.media.application.storage;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import java.net.URI;
import java.time.Instant;

public record ImageReadAuthorization(URI readUrl, Instant expiresAt) {

  public ImageReadAuthorization {
    requireNotNull(readUrl, "readUrl");
    requireNotNull(expiresAt, "expiresAt");
  }
}
