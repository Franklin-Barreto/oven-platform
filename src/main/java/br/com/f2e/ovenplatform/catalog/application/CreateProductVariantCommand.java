package br.com.f2e.ovenplatform.catalog.application;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductVariantCommand(UUID imageId, String name, BigDecimal price) {}
