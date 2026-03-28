package br.com.f2e.ovenplatform.tenant.domain;

import br.com.f2e.ovenplatform.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenants")
public class Tenant extends BaseEntity {

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private Status status;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private Plan plan;

  @SuppressWarnings("unused")
  protected Tenant() {}

  public Tenant(String name, Plan plan) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Name can't be null or blank");
    }
    if (plan == null) {
      throw new IllegalArgumentException("Plan can't be empty");
    }

    this.name = name;
    this.plan = plan;
    this.status = Status.ACTIVE;
  }

  public String getName() {
    return name;
  }

  public Status getStatus() {
    return status;
  }

  public Plan getPlan() {
    return plan;
  }
}
