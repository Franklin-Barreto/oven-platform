package br.com.f2e.ovenplatform.identity.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.identity.domain.TenantMembership;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import br.com.f2e.ovenplatform.identity.domain.User;
import br.com.f2e.ovenplatform.identity.domain.UserRole;
import br.com.f2e.ovenplatform.identity.domain.exception.TenantMembershipInactiveException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  private static final UUID TENANT_ID = UUID.randomUUID();

  @Mock private AuthenticationManager authenticationManager;
  @Mock private AccessTokenService accessTokenService;
  @Mock private TenantMembershipRepository tenantMembershipRepository;

  private AuthService authService;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    authService =
        new AuthService(authenticationManager, accessTokenService, tenantMembershipRepository);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldAuthenticateUserAndReturnAccessToken() {
    var user = new User(TENANT_ID, "john@email.com", "password-hash", UserRole.MEMBER);

    var authenticated = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

    when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);
    when(accessTokenService.generateToken(
            TENANT_ID, user.getId(), TenantMembershipRole.MEMBER.name()))
        .thenReturn("jwt-token");
    when(tenantMembershipRepository.findByUserIdAndTenantId(user.getId(), TENANT_ID))
        .thenReturn(Optional.of(createTenantMembership(user, TenantMembershipRole.MEMBER)));

    var token = authService.login(TENANT_ID, "john@email.com", "123456");

    assertEquals("jwt-token", token);
    assertSame(authenticated, SecurityContextHolder.getContext().getAuthentication());

    verify(accessTokenService)
        .generateToken(TENANT_ID, user.getId(), TenantMembershipRole.MEMBER.name());
  }

  @Test
  void shouldAuthenticateUsingEmailAndPassword() {
    var user = new User(UUID.randomUUID(), "john@email.com", "password-hash", UserRole.ADMIN);
    var authenticated = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

    when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);
    when(accessTokenService.generateToken(
            TENANT_ID, user.getId(), TenantMembershipRole.ADMIN.name()))
        .thenReturn("jwt-token");
    when(tenantMembershipRepository.findByUserIdAndTenantId(user.getId(), TENANT_ID))
        .thenReturn(Optional.of(createTenantMembership(user, TenantMembershipRole.ADMIN)));

    authService.login(TENANT_ID, "john@email.com", "123456");

    verify(authenticationManager)
        .authenticate(
            argThat(
                authentication ->
                    Objects.equals(authentication.getPrincipal(), "john@email.com")
                        && Objects.equals(authentication.getCredentials(), "123456")
                        && !authentication.isAuthenticated()));
  }

  @Test
  void shouldNotGenerateTokenWhenAuthenticationFails() {
    when(authenticationManager.authenticate(any(Authentication.class)))
        .thenThrow(new BadCredentialsException("Bad credentials"));

    assertThrows(
        BadCredentialsException.class,
        () -> authService.login(TENANT_ID, "john@email.com", "wrong-password"));

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verifyNoInteractions(accessTokenService);
  }

  @Test
  void shouldFailWhenAuthenticatedPrincipalIsNotUser() {
    var authenticated = new UsernamePasswordAuthenticationToken("john@email.com", null, List.of());

    when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);

    assertThrows(
        IllegalStateException.class,
        () -> authService.login(TENANT_ID, "john@email.com", "123456"));

    verifyNoInteractions(accessTokenService);
  }

  @Test
  void shouldFailWhenTenantMembershipIsInactive() {

    var user = new User(UUID.randomUUID(), "john@email.com", "password-hash", UserRole.ADMIN);
    var authenticated = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    var tenantMembership = createTenantMembership(user, TenantMembershipRole.ADMIN);
    tenantMembership.inactiveTenantMembership();

    when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);
    when(tenantMembershipRepository.findByUserIdAndTenantId(user.getId(), TENANT_ID))
        .thenReturn(Optional.of(tenantMembership));

    assertThatThrownBy(() -> authService.login(TENANT_ID, "john@email.com", "123456"))
        .isInstanceOf(TenantMembershipInactiveException.class)
        .hasMessage("Tenant membership is inactive.");
  }

  private TenantMembership createTenantMembership(User user, TenantMembershipRole role) {
    return new TenantMembership(user, AuthServiceTest.TENANT_ID, role);
  }
}
