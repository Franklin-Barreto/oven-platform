package br.com.f2e.ovenplatform.media.infrastructure.web.dto;

import br.com.f2e.ovenplatform.media.application.delivery.PublicImageLocation;
import java.net.URI;

public record PublicImageLocationResponse(URI url) {

  public static PublicImageLocationResponse from(PublicImageLocation location) {
    return new PublicImageLocationResponse(location.url());
  }
}
