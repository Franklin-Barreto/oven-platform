package br.com.f2e.ovenplatform.media.infrastructure.delivery;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotBlank;

import br.com.f2e.ovenplatform.media.application.delivery.ImageDelivery;
import br.com.f2e.ovenplatform.media.application.delivery.PublicImageLocation;
import java.net.URI;
import org.springframework.stereotype.Component;

@Component
public class PublicUrlImageDeliveryAdapter implements ImageDelivery {

  private final URI baseUrl;

  public PublicUrlImageDeliveryAdapter(MediaDeliveryProperties properties) {
    var configuredBaseUrl = properties.baseUrl().toString();
    this.baseUrl =
        URI.create(configuredBaseUrl.endsWith("/") ? configuredBaseUrl : configuredBaseUrl + "/");
  }

  @Override
  public PublicImageLocation resolvePublicLocation(String objectKey) {
    var relativeObjectKey = requireNotBlank(objectKey, "objectKey").replaceFirst("^/+", "");
    return new PublicImageLocation(baseUrl.resolve(relativeObjectKey));
  }
}
