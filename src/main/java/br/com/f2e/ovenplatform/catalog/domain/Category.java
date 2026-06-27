package br.com.f2e.ovenplatform.catalog.domain;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireMinimumSize;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import br.com.f2e.ovenplatform.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "categories")
public class Category extends BaseEntity {

  @Column(nullable = false, length = 80)
  private String name;

  @Column(nullable = false)
  private UUID tenantId;

  @Column(nullable = false)
  private boolean active;

  @SuppressWarnings("unused")
  protected Category() {}

  public Category(String name, UUID tenantId) {
    this.name = requireMinimumSize(name, "name", 5);
    this.tenantId = requireNotNull(tenantId, "tenantId");
    this.active = true;
  }

  public String getName() {
    return name;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public boolean isActive() {
    return active;
  }

  public void rename(String name) {
    this.name = requireMinimumSize(name, "name", 5);
  }

  public void activate() {
    active = true;
  }

  public void deactivate() {
    active = false;
  }
}
