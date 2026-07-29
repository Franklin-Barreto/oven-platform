package br.com.f2e.ovenplatform.media.infrastructure.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AwsMediaPropertiesTest {

  private static final Duration POSITIVE_TTL = Duration.ofMinutes(10);

  @Test
  void shouldNormalizeTextProperties() {
    var properties = new AwsMediaProperties(" media-bucket ", " us-east-1 ", POSITIVE_TTL);

    assertThat(properties.bucket()).isEqualTo("media-bucket");
    assertThat(properties.region()).isEqualTo("us-east-1");
  }

  @ParameterizedTest(name = "rejects invalid AWS media configuration: {0}")
  @MethodSource("invalidProperties")
  void shouldRejectInvalidConfiguration(
      String expectedField, String bucket, String region, Duration uploadAuthorizationTtl) {

    assertThatThrownBy(() -> new AwsMediaProperties(bucket, region, uploadAuthorizationTtl))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(expectedField);
  }

  private static Stream<Arguments> invalidProperties() {
    return Stream.of(
        Arguments.of("bucket", null, "us-east-1", POSITIVE_TTL),
        Arguments.of("bucket", " ", "us-east-1", POSITIVE_TTL),
        Arguments.of("region", "media-bucket", null, POSITIVE_TTL),
        Arguments.of("region", "media-bucket", " ", POSITIVE_TTL),
        Arguments.of("uploadAuthorizationTtl", "media-bucket", "us-east-1", null),
        Arguments.of("uploadAuthorizationTtl", "media-bucket", "us-east-1", Duration.ZERO),
        Arguments.of(
            "uploadAuthorizationTtl", "media-bucket", "us-east-1", Duration.ofSeconds(-1)));
  }
}
