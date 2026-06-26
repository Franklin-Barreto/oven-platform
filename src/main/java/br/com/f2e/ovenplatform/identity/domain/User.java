package br.com.f2e.ovenplatform.identity.domain;

import static br.com.f2e.ovenplatform.identity.domain.validation.EmailNormalizer.normalize;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotBlank;

import br.com.f2e.ovenplatform.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Table(name = "users")
@Entity
public class User extends BaseEntity {

  @Column(nullable = false)
  private String email;

  @Column(nullable = false)
  private String passwordHash;

  @SuppressWarnings("unused")
  protected User() {}

  public User(String email, String passwordHash) {

    this.email = normalize(email);
    this.passwordHash = requireNotBlank(passwordHash, "passwordHash");
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }
}
