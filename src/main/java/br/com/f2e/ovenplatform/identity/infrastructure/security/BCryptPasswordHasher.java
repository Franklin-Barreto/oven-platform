package br.com.f2e.ovenplatform.identity.infrastructure.security;

import br.com.f2e.ovenplatform.identity.application.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordHasher implements PasswordHasher {

  private final PasswordEncoder passwordEncoder;

  public BCryptPasswordHasher(PasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public String hash(String rawPassword) {
    return passwordEncoder.encode(rawPassword);
  }
}
