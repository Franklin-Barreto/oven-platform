package br.com.f2e.ovenplatform.catalog.infrastructure.web.product;

import jakarta.validation.constraints.NotNull;

public record ChangeProductVariantStatusRequest(@NotNull Boolean active) {}
