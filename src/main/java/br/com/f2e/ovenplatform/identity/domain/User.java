package br.com.f2e.ovenplatform.identity.domain;

import static br.com.f2e.ovenplatform.identity.domain.validation.Preconditions.normalizeEmail;
import static br.com.f2e.ovenplatform.identity.domain.validation.Preconditions.requireNotBlank;
import static br.com.f2e.ovenplatform.identity.domain.validation.Preconditions.requireNotNull;

import br.com.f2e.ovenplatform.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Table(
    name = "users",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_users_tenant_id_email",
          columnNames = {"tenant_id", "email"})
    })
@Entity
public class User extends BaseEntity implements UserDetails {

  @Column(nullable = false)
  private UUID tenantId;

  @Column(nullable = false)
  private String email;

  @Column(nullable = false)
  private String passwordHash;

  @NotNull
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

  public UserRole getRole() {
    return role;
  }

  public UserStatus getStatus() {
    return status;
  }

  @Override
  public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority(role.name()));
  }

  @Override
  public String getPassword() {
    return passwordHash;
  }

  @Override
  public @NonNull String getUsername() {
    return getEmail();
  }
}
