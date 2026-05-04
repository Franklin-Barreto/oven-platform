package br.com.f2e.ovenplatform.identity.infrastructure.security;

import br.com.f2e.ovenplatform.identity.application.AccessTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService implements AccessTokenService {

  private final SecretKey key;
  private final long expirationMinutes;

  public JwtService(
      @Value("${jwt.secret}") String jwtSecret,
      @Value("${jwt.expiration-minutes}") long expirationMinutes) {
    key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    this.expirationMinutes = expirationMinutes;
  }

  @Override
  public String generateToken(UUID subject, String roleName) {

    Instant instant = Instant.now();
    return Jwts.builder()
        .subject(subject.toString())
        .issuedAt(new Date(instant.toEpochMilli()))
        .expiration(new Date(instant.plus(expirationMinutes, ChronoUnit.MINUTES).toEpochMilli()))
        .signWith(key)
        .claim("role", roleName)
        .compact();
  }

  public Claims parseClaims(String jwt) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(jwt).getPayload();
  }
}
