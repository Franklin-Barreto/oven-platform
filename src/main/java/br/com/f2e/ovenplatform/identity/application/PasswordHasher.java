package br.com.f2e.ovenplatform.identity.application;

public interface PasswordHasher {
  String hash(String rawPassword);
}
