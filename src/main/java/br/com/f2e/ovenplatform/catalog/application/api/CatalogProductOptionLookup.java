package br.com.f2e.ovenplatform.catalog.application.api;

import java.util.Optional;
import java.util.UUID;

public interface CatalogProductOptionLookup {

  Optional<ActiveProductOptionConfiguration> findActiveProductOptionConfiguration(
      UUID tenantId, UUID productId);
}
