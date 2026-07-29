package br.com.f2e.ovenplatform.catalog.application;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductCommand(
    UUID categoryId,
    UUID imageId,
    String name,
    String description,
    BigDecimal price,
    boolean active) {}
