package br.com.f2e.ovenplatform.media.infrastructure.delivery;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import java.net.URI;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oven.media.delivery")
public record MediaDeliveryProperties(URI baseUrl) {

  private static final Set<String> SUPPORTED_SCHEMES = Set.of("http", "https");

  public MediaDeliveryProperties {
    requireNotNull(baseUrl, "baseUrl");

    if (!baseUrl.isAbsolute()
        || baseUrl.getHost() == null
        || !SUPPORTED_SCHEMES.contains(baseUrl.getScheme().toLowerCase())) {
      throw new IllegalArgumentException("baseUrl must be an absolute HTTP or HTTPS URL");
    }
  }
}
