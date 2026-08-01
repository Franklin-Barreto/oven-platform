package br.com.f2e.ovenplatform.catalog.application.product;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductOptionDetailResult(
    UUID id, String name, BigDecimal priceAdjustment, int displayPosition) {}
