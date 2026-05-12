package br.com.f2e.ovenplatform.shared.infrastructure.persistence.test;

import br.com.f2e.ovenplatform.shared.domain.BaseEntity;
import java.util.UUID;
import org.springframework.test.util.ReflectionTestUtils;

public final class EntityIdTestUtils {

  private EntityIdTestUtils() {}

  public static <T extends BaseEntity> T withId(T entity, UUID id) {
    ReflectionTestUtils.setField(entity, "id", id);
    return entity;
  }

  public static <T extends BaseEntity> T withRandomId(T entity) {
    return withId(entity, UUID.randomUUID());
  }
}
