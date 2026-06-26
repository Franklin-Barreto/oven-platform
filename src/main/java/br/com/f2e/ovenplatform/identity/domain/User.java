package br.com.f2e.ovenplatform.identity.domain;

import static br.com.f2e.ovenplatform.identity.domain.validation.EmailNormalizer.normalize;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotBlank;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import br.com.f2e.ovenplatform.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

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
  private UserStatus status;

  @SuppressWarnings("unused")
  protected User() {}

  public User(UUID tenantId, String email, String passwordHash) {

    this.tenantId = requireNotNull(tenantId, "tenantId");
    this.email = normalize(email);
    this.passwordHash = requireNotBlank(passwordHash, "passwordHash");
    this.status = UserStatus.ACTIVE;
  }

  public String getEmail() {
    return email;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public UserStatus getStatus() {
    return status;
  }

  public String getPassword() {
    return passwordHash;
  }
}
