package br.com.f2e.ovenplatform.catalog.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductVariantRequest(
    UUID imageId, @NotBlank @Size(max = 80) String name, @NotNull @Positive BigDecimal price) {}
