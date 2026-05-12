package br.com.f2e.ovenplatform.shared.infrastructure.web.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.springframework.test.web.servlet.MvcResult;

public final class LocationHeaderAssertions {

  private LocationHeaderAssertions() {}

  public static void assertLocationPath(MvcResult result, String expectedPath) {
    var location = result.getResponse().getHeader("Location");

    assertThat(location).isNotNull();

    var uri = URI.create(location);

    assertThat(uri.getPath()).isEqualTo(expectedPath);
    assertThat(uri.getQuery()).isNull();
  }
}
