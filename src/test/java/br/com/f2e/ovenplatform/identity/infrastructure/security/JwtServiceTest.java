package br.com.f2e.ovenplatform.identity.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.f2e.ovenplatform.identity.domain.UserRole;
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
  private JwtService jwtService;
  private final UUID subject = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    jwtService = new JwtService(TEST_SECRET, 30);
  }

  @Test
  void shouldGenerateAndParseToken() {

    var token = jwtService.generateToken(subject, UserRole.MEMBER.name());
    var claims = jwtService.parseClaims(token);

    Date issuedAt = claims.getIssuedAt();
    Date expiration = claims.getExpiration();

    assertEquals(subject, UUID.fromString(claims.getSubject()));
    assertEquals(UserRole.MEMBER.name(), claims.get("role"));
    assertNotNull(issuedAt);
    assertNotNull(expiration);
    assertTrue(issuedAt.before(expiration));
  }

  @Test
  void shouldFailWhenTokenSignatureIsInvalid() {

    var token = jwtService.generateToken(subject, UserRole.MEMBER.name());
    var jwtServiceWithDifferentSecret =
        new JwtService(Encoders.BASE64.encode(Jwts.SIG.HS256.key().build().getEncoded()), 20);

    assertThrows(JwtException.class, () -> jwtServiceWithDifferentSecret.parseClaims(token));
  }

  @Test
  void shouldFailWhenTokenIsExpired() {

    var expiredJwtService = new JwtService(TEST_SECRET, -1);
    var token = expiredJwtService.generateToken(subject, UserRole.ADMIN.name());

    assertThrows(ExpiredJwtException.class, () -> expiredJwtService.parseClaims(token));
  }
}
