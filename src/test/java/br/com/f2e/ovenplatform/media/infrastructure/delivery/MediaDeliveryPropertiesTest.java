package br.com.f2e.ovenplatform.media.infrastructure.delivery;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class MediaDeliveryPropertiesTest {

  @ParameterizedTest
  @MethodSource("invalidBaseUrls")
  void shouldRejectInvalidBaseUrl(URI baseUrl) {
    assertThatThrownBy(() -> new MediaDeliveryProperties(baseUrl))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("baseUrl");
  }

  private static Stream<URI> invalidBaseUrls() {
    return Stream.of(null, URI.create("/media/"), URI.create("ftp://media.oven-platform.com"));
  }
}
