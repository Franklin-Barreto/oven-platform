package br.com.f2e.ovenplatform.kitchen.infrastructure.kafka.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderCreatedItemPayload(UUID productId, String productName, int quantity) {}
