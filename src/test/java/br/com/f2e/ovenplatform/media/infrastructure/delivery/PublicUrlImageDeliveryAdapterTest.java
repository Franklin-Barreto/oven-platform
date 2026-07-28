package br.com.f2e.ovenplatform.media.infrastructure.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.junit.jupiter.api.Test;

class PublicUrlImageDeliveryAdapterTest {

  private static final String OBJECT_KEY =
      "tenants/a6210129-f1d5-4942-8d0a-b144e518aecc/images/"
          + "7d877954-28f7-483d-9c21-60d13ec17e80.webp";

  @Test
  void shouldResolvePublicLocationFromConfiguredBaseUrl() {
    var delivery =
        new PublicUrlImageDeliveryAdapter(
            new MediaDeliveryProperties(URI.create("https://media.oven-platform.com")));

    var location = delivery.resolvePublicLocation(OBJECT_KEY);

    assertThat(location.url())
        .isEqualTo(URI.create("https://media.oven-platform.com/" + OBJECT_KEY));
  }

  @Test
  void shouldAvoidDuplicateSlashWhenBaseUrlAndObjectKeyContainBoundarySlashes() {
    var delivery =
        new PublicUrlImageDeliveryAdapter(
            new MediaDeliveryProperties(URI.create("https://media.oven-platform.com/")));

    var location = delivery.resolvePublicLocation("/" + OBJECT_KEY);

    assertThat(location.url())
        .isEqualTo(URI.create("https://media.oven-platform.com/" + OBJECT_KEY));
  }

  @Test
  void shouldRejectBlankObjectKey() {
    var delivery =
        new PublicUrlImageDeliveryAdapter(
            new MediaDeliveryProperties(URI.create("https://media.oven-platform.com")));

    assertThatThrownBy(() -> delivery.resolvePublicLocation(" "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("objectKey must not be blank");
  }
}
