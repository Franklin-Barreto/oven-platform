package br.com.f2e.ovenplatform.identity.domain;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import br.com.f2e.ovenplatform.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(
    name = "tenant_memberships",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_tenant_memberships_tenant_id_user_id",
          columnNames = {"tenant_id", "user_id"})
    })
public class TenantMembership extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private TenantMembershipRole role;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private TenantMembershipStatus status;

  protected TenantMembership() {}

  public TenantMembership(User user, UUID tenantId, TenantMembershipRole role) {
    this.user = requireNotNull(user, "user");
    this.tenantId = requireNotNull(tenantId, "tenantId");
    this.role = requireNotNull(role, "role");
    this.status = TenantMembershipStatus.ACTIVE;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public TenantMembershipRole getRole() {
    return role;
  }

  public TenantMembershipStatus getStatus() {
    return status;
  }

  public User getUser() {
    return user;
  }

  public void deactivate() {
    this.status = TenantMembershipStatus.INACTIVE;
  }
}
