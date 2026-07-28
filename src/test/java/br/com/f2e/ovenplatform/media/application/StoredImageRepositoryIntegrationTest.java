package br.com.f2e.ovenplatform.media.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.f2e.ovenplatform.media.domain.StoredImage;
import br.com.f2e.ovenplatform.media.domain.StoredImageStatus;
import br.com.f2e.ovenplatform.media.infrastructure.persistence.JpaStoredImageRepositoryAdapter;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(JpaStoredImageRepositoryAdapter.class)
class StoredImageRepositoryIntegrationTest extends DataJpaIntegrationTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID ANOTHER_TENANT_ID = UUID.randomUUID();
  private static final String CHECKSUM = "0t/CUcGnJF1Ot9leX4FUcsbbz37maQu9fBkS9He2wio=";

  @Autowired private StoredImageRepository repository;

  @Test
  void shouldPersistAndReloadPendingImage() {
    var image = repository.save(pendingImage(TENANT_ID));

    flushAndClear();

    var reloaded = repository.findByIdAndTenantId(image.getId(), TENANT_ID);

    assertThat(reloaded)
        .isPresent()
        .get()
        .satisfies(
            persisted -> {
              assertThat(persisted.getId()).isEqualTo(image.getId());
              assertThat(persisted.getTenantId()).isEqualTo(TENANT_ID);
              assertThat(persisted.getObjectKey()).isEqualTo(image.getObjectKey());
              assertThat(persisted.getContentType()).isEqualTo("image/webp");
              assertThat(persisted.getSizeBytes()).isEqualTo(1024);
              assertThat(persisted.getChecksum()).isEqualTo(CHECKSUM);
              assertThat(persisted.getStatus()).isEqualTo(StoredImageStatus.PENDING);
              assertThat(persisted.getCreatedAt()).isNotNull();
              assertThat(persisted.getUpdatedAt()).isNotNull();
            });
  }

  @Test
  void shouldNotFindImageThroughAnotherTenant() {
    var image = repository.save(pendingImage(TENANT_ID));

    flushAndClear();

    var found = repository.findByIdAndTenantId(image.getId(), ANOTHER_TENANT_ID);

    assertThat(found).isEmpty();
  }

  @Test
  void shouldPersistAvailableTransition() {
    var image = repository.save(pendingImage(TENANT_ID));
    image.confirm("image/webp", 1024, CHECKSUM);
    repository.save(image);

    flushAndClear();

    var reloaded = repository.findByIdAndTenantId(image.getId(), TENANT_ID).orElseThrow();

    assertThat(reloaded.getStatus()).isEqualTo(StoredImageStatus.AVAILABLE);
    assertThat(reloaded.isAvailable()).isTrue();
  }

  @Test
  void shouldFindOnlyPendingImagesCreatedBeforeThreshold() {
    var pending = repository.save(pendingImage(TENANT_ID));
    var available = pendingImage(TENANT_ID);
    available.confirm("image/webp", 1024, CHECKSUM);
    repository.save(available);

    flushAndClear();

    var images = repository.findPendingCreatedBefore(Instant.now().plusSeconds(5));

    assertThat(images)
        .extracting(StoredImage::getId)
        .contains(pending.getId())
        .doesNotContain(available.getId());
  }

  @Test
  void shouldEnforceUniqueObjectKey() {
    var objectKey = objectKey();
    repository.save(pendingImage(TENANT_ID, objectKey));
    repository.save(pendingImage(ANOTHER_TENANT_ID, objectKey));

    assertThatThrownBy(this::flushAndClear).isInstanceOf(ConstraintViolationException.class);
  }

  @Test
  void shouldHardDeleteStoredImage() {
    var image = repository.save(pendingImage(TENANT_ID));
    var imageId = image.getId();

    repository.delete(image);
    flushAndClear();

    assertThat(repository.findByIdAndTenantId(imageId, TENANT_ID)).isEmpty();
  }

  private StoredImage pendingImage(UUID tenantId) {
    return pendingImage(tenantId, objectKey());
  }

  private StoredImage pendingImage(UUID tenantId, String objectKey) {
    return StoredImage.pending(tenantId, objectKey, "image/webp", 1024, CHECKSUM);
  }

  private String objectKey() {
    return "tenants/%s/images/%s.webp".formatted(TENANT_ID, UUID.randomUUID());
  }
}
