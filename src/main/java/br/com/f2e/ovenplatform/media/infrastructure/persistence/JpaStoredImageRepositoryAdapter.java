package br.com.f2e.ovenplatform.media.infrastructure.persistence;

import br.com.f2e.ovenplatform.media.application.StoredImageRepository;
import br.com.f2e.ovenplatform.media.domain.StoredImage;
import br.com.f2e.ovenplatform.media.domain.StoredImageStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaStoredImageRepositoryAdapter implements StoredImageRepository {

  private final SpringDataStoredImageRepository repository;

  public JpaStoredImageRepositoryAdapter(SpringDataStoredImageRepository repository) {
    this.repository = repository;
  }

  @Override
  public StoredImage save(StoredImage storedImage) {
    return repository.save(storedImage);
  }

  @Override
  public Optional<StoredImage> findByIdAndTenantId(UUID id, UUID tenantId) {
    return repository.findByIdAndTenantId(id, tenantId);
  }

  @Override
  public void delete(StoredImage storedImage) {
    repository.delete(storedImage);
    repository.flush();
  }

  @Override
  public List<StoredImage> findPendingCreatedBefore(Instant time) {
    return repository.findByStatusAndCreatedAtBefore(StoredImageStatus.PENDING, time);
  }

  @Override
  public List<StoredImage> findAllByTenantIdAndIdIn(UUID tenantId, Collection<UUID> ids) {
    return repository.findByTenantIdAndIdIn(tenantId, ids);
  }
}
