package br.com.f2e.ovenplatform.catalog.application;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductVariantCommand(
    UUID imageId, String name, BigDecimal price, boolean active) {}
