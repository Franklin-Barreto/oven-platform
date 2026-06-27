package br.com.f2e.ovenplatform.catalog.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductRequest(
    @NotNull UUID categoryId,
    @NotBlank @Size(min = 5, message = "name must have at least 5 characters") String name,
    @Size(max = 500, message = "description must have at most 500 characters") String description,
    @NotNull @Positive BigDecimal price,
    @NotNull Boolean active) {}
