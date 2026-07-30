package br.com.f2e.ovenplatform.catalog.infrastructure.web;

import br.com.f2e.ovenplatform.catalog.application.product.CreateProductCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductRequest(
    @NotNull UUID categoryId,
    @NotNull UUID imageId,
    @NotBlank @Size(min = 5, message = "name must have at least 5 characters") String name,
    @Size(max = 500, message = "description must have at most 500 characters") String description,
    @NotNull @Positive BigDecimal price) {

  public CreateProductCommand toCommand() {
    return new CreateProductCommand(categoryId, imageId, name, description, price);
  }
}
