package br.com.f2e.ovenplatform.media.infrastructure.delivery;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "oven.media.delivery")
public record MediaDeliveryProperties(@NotNull URI baseUrl) {}
