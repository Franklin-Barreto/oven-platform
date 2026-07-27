package br.com.f2e.ovenplatform.media.application.delivery;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import java.net.URI;

public record PublicImageLocation(URI url) {

  public PublicImageLocation {
    requireNotNull(url, "url");
  }
}
