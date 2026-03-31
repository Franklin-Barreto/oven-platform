package br.com.f2e.ovenplatform.identity.domain;

import br.com.f2e.ovenplatform.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

import static br.com.f2e.ovenplatform.identity.domain.validation.Preconditions.normalizeEmail;
import static br.com.f2e.ovenplatform.identity.domain.validation.Preconditions.requireNotBlank;
import static br.com.f2e.ovenplatform.identity.domain.validation.Preconditions.requireNotNull;

@Table(
    name = "users",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_users_tenant_id_email",
          columnNames = {"tenant_id", "email"})
    })
@Entity
public class User extends BaseEntity {

  @Column(nullable = false)
  private UUID tenantId;

  @Column(nullable = false)
  private String email;

  @Column(nullable = false)
  private String passwordHash;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private UserRole role;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private UserStatus status;

  @SuppressWarnings("unused")
  protected User() {}

  public User(UUID tenantId, String email, String passwordHash, UserRole role) {

    requireNotNull(tenantId, "tenantId");
    requireNotBlank(passwordHash, "passwordHash");
    requireNotNull(role, "role");

    this.tenantId = tenantId;
    this.email = normalizeEmail(email);
    this.passwordHash = passwordHash;
    this.role = role;
    this.status = UserStatus.ACTIVE;
  }

  public String getEmail() {
    return email;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public UserRole getRole() {
    return role;
  }

  public UserStatus getStatus() {
    return status;
  }
}
