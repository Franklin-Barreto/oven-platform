package br.com.f2e.ovenplatform.catalog.infrastructure.web;

import jakarta.validation.constraints.NotNull;

public record ChangeProductVariantStatusRequest(@NotNull Boolean active) {}
