package br.com.f2e.ovenplatform.catalog.infrastructure.web.option;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateOptionRequest(
    @NotBlank @Size(max = 80) String name, @NotNull @PositiveOrZero BigDecimal priceAdjustment) {}
