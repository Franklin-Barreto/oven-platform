package br.com.f2e.ovenplatform.identity.domain;

import br.com.f2e.ovenplatform.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Table(
    name = "users",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_users_tenant_id_email",
          columnNames = {"tenant_id", "email"})
    })
@Entity
public class User extends BaseEntity {
  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

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
    requireNotBlank(email, "email");
    requireNotBlank(passwordHash, "passwordHash");
    requireNotNull(role, "role");

    var normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
    if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
      throw new IllegalArgumentException("Invalid email format");
    }

    this.tenantId = tenantId;
    this.email = normalizedEmail;
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

  private static void requireNotNull(Object field, String fieldName) {
    if (field == null)
      throw new IllegalArgumentException("%s must not be null".formatted(fieldName));
  }

  private static void requireNotBlank(String field, String fieldName) {
    requireNotNull(field, fieldName);
    if (field.isBlank())
      throw new IllegalArgumentException("%s must not be blank".formatted(fieldName));
  }
}
