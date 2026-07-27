package br.com.f2e.ovenplatform.media.application;

import br.com.f2e.ovenplatform.media.domain.StoredImage;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoredImageRepository {

  StoredImage save(StoredImage storedImage);

  Optional<StoredImage> findByIdAndTenantId(UUID id, UUID tenantId);

  void delete(StoredImage storedImage);

  List<StoredImage> findPendingCreatedBefore(Instant createdBefore);
}
