package br.com.f2e.ovenplatform.payment.infrastructure.stripe;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotBlank;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oven.payment.stripe")
public record StripeProperties(String secretKey, URI successUrl, URI cancelUrl) {

  public StripeProperties {
    secretKey = requireNotBlank(secretKey, "secretKey");
    validateRedirectUrl(successUrl, "successUrl");
    validateRedirectUrl(cancelUrl, "cancelUrl");
  }

  private static void validateRedirectUrl(URI uri, String fieldName) {
    requireNotNull(uri, fieldName);
    var scheme = uri.getScheme();
    var host = uri.getHost();
    var isHttps = "https".equalsIgnoreCase(scheme);
    var isLocalHttp = "http".equalsIgnoreCase(scheme) && "localhost".equalsIgnoreCase(host);

    if (!isHttps && !isLocalHttp) {
      throw new IllegalArgumentException(
          fieldName + " must use HTTPS, except for HTTP on localhost");
    }
  }
}
