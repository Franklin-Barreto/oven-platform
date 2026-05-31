package br.com.f2e.ovenplatform.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private static final String TEST_SECRET =
      Encoders.BASE64.encode(Jwts.SIG.HS256.key().build().getEncoded());
  private static final UUID SUBJECT = UUID.randomUUID();
  private static final UUID TENANT_ID = UUID.randomUUID();

  private JwtService jwtService;

  @BeforeEach
  void setUp() {
    jwtService = new JwtService(TEST_SECRET, 30);
  }

  @Test
  void shouldGenerateAndParseToken() {

    var token = jwtService.generateToken(TENANT_ID, SUBJECT, TenantMembershipRole.MEMBER.name());
    var claims = jwtService.parseClaims(token);

    Date issuedAt = claims.getIssuedAt();
    Date expiration = claims.getExpiration();

    assertThat(UUID.fromString(claims.getSubject())).isEqualTo(SUBJECT);
    assertThat(UUID.fromString(claims.get("tenantId", String.class))).isEqualTo(TENANT_ID);
    assertThat(claims.get("role", String.class)).isEqualTo(TenantMembershipRole.MEMBER.name());
    assertThat(issuedAt).isNotNull();
    assertThat(expiration).isNotNull();
    assertThat(issuedAt).isBefore(expiration);
  }

  @Test
  void shouldFailWhenTokenSignatureIsInvalid() {

    var token = jwtService.generateToken(TENANT_ID, SUBJECT, TenantMembershipRole.MEMBER.name());
    var jwtServiceWithDifferentSecret =
        new JwtService(Encoders.BASE64.encode(Jwts.SIG.HS256.key().build().getEncoded()), 20);

    assertThrows(JwtException.class, () -> jwtServiceWithDifferentSecret.parseClaims(token));
  }

  @Test
  void shouldFailWhenTokenIsExpired() {

    var expiredJwtService = new JwtService(TEST_SECRET, -1);
    var token =
        expiredJwtService.generateToken(TENANT_ID, SUBJECT, TenantMembershipRole.ADMIN.name());

    assertThrows(ExpiredJwtException.class, () -> expiredJwtService.parseClaims(token));
  }
}
