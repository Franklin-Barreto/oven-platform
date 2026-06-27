package br.com.f2e.ovenplatform.catalog.infrastructure.web;

import br.com.f2e.ovenplatform.catalog.domain.Category;
import java.util.UUID;

public record CategoryResponse(UUID id, UUID tenantId, String name, boolean active) {

  public static CategoryResponse from(Category category) {
    return new CategoryResponse(
        category.getId(), category.getTenantId(), category.getName(), category.isActive());
  }
}
