package br.com.f2e.ovenplatform.media.infrastructure.persistence;

import br.com.f2e.ovenplatform.media.domain.StoredImage;
import br.com.f2e.ovenplatform.media.domain.StoredImageStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataStoredImageRepository extends JpaRepository<StoredImage, UUID> {

  Optional<StoredImage> findByIdAndTenantId(UUID id, UUID tenantId);

  List<StoredImage> findByStatusAndCreatedAtBefore(StoredImageStatus status, Instant time);
}
