package br.com.f2e.ovenplatform.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TenantMembershipTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final String USER_EMAIL = "user.email@outlook.com";
  private static final String RAW_PASSWORD = "my-secret-password";

  @Test
  void shouldCreateTenantMembership() {
    var user = createUser();

    var tenantMembership = new TenantMembership(user, TENANT_ID, TenantMembershipRole.OWNER);

    assertThat(tenantMembership.getTenantId()).isEqualTo(TENANT_ID);
    assertThat(tenantMembership.getUser()).isEqualTo(user);
    assertThat(tenantMembership.getUser().getEmail()).isEqualTo(USER_EMAIL);
    assertThat(tenantMembership.getRole()).isEqualTo(TenantMembershipRole.OWNER);
    assertThat(tenantMembership.getStatus()).isEqualTo(TenantMembershipStatus.ACTIVE);
  }

  @ParameterizedTest
  @MethodSource("invalidData")
  void shouldRejectTenantMembershipWithInvalidData(
      User user, UUID tenantId, TenantMembershipRole role, String message) {
    assertThatThrownBy(() -> new TenantMembership(user, tenantId, role))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(message);
  }

  private static User createUser() {
    return new User(TENANT_ID, USER_EMAIL, RAW_PASSWORD, UserRole.OWNER);
  }

  private static Stream<Arguments> invalidData() {
    return Stream.of(
        Arguments.of(null, TENANT_ID, TenantMembershipRole.OWNER, "user must not be null"),
        Arguments.of(createUser(), null, TenantMembershipRole.OWNER, "tenantId must not be null"),
        Arguments.of(createUser(), TENANT_ID, null, "role must not be null"));
  }
}
