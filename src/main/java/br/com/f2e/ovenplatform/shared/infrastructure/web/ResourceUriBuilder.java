package br.com.f2e.ovenplatform.shared.infrastructure.web;

import java.net.URI;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public final class ResourceUriBuilder {

  private ResourceUriBuilder() {}

  public static URI buildLocation(Object resourceId) {
    return ServletUriComponentsBuilder.fromCurrentRequestUri()
        .path("/{id}")
        .buildAndExpand(resourceId)
        .toUri();
  }
}
