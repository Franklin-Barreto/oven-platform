package br.com.f2e.ovenplatform.orders.application;

import java.util.UUID;

public record OrderableProductSelection(UUID productId, UUID variantId) {}
