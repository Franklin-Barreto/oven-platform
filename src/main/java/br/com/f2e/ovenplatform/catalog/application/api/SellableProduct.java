package br.com.f2e.ovenplatform.catalog.application.api;

import java.math.BigDecimal;
import java.util.UUID;

public record SellableProduct(UUID productId, BigDecimal price) {}
