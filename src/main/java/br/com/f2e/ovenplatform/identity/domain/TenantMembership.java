package br.com.f2e.ovenplatform.identity.domain;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotEmptyAndWithoutNulls;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import br.com.f2e.ovenplatform.shared.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
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

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
      name = "tenant_membership_roles",
      joinColumns = @JoinColumn(name = "membership_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private Set<TenantMembershipRole> roles = new HashSet<>();

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private TenantMembershipStatus status;

  protected TenantMembership() {}

  private TenantMembership(User user, UUID tenantId, Set<TenantMembershipRole> roles) {
    this.user = requireNotNull(user, "user");
    this.tenantId = requireNotNull(tenantId, "tenantId");
    this.roles = validateRoles(roles);
    this.status = TenantMembershipStatus.ACTIVE;
  }

  public static TenantMembership owner(User user, UUID tenantId) {
    return new TenantMembership(user, tenantId, Set.of(TenantMembershipRole.OWNER));
  }

  public static TenantMembership staff(User user, UUID tenantId, Set<TenantMembershipRole> roles) {
    return new TenantMembership(user, tenantId, validateOperationalRoles(roles));
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public Set<TenantMembershipRole> getRoles() {
    return Set.copyOf(roles);
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

  public void activate() {
    this.status = TenantMembershipStatus.ACTIVE;
  }

  public void changeOperationalRolesTo(Set<TenantMembershipRole> roles) {
    this.roles = validateOperationalRoles(roles);
  }

  private static Set<TenantMembershipRole> validateRoles(Set<TenantMembershipRole> roles) {
    requireNotEmptyAndWithoutNulls(roles, "roles");
    return EnumSet.copyOf(roles);
  }

  private static Set<TenantMembershipRole> validateOperationalRoles(
      Set<TenantMembershipRole> roles) {

    var validatedRoles = validateRoles(roles);

    if (validatedRoles.contains(TenantMembershipRole.OWNER)) {
      throw new IllegalArgumentException("Staff membership cannot contain OWNER");
    }

    return validatedRoles;
  }
}
