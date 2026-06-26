package br.com.f2e.ovenplatform.identity.application;

import static br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.EntityIdTestUtils.withId;
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
import br.com.f2e.ovenplatform.identity.domain.exception.TenantAccessDeniedException;
import br.com.f2e.ovenplatform.identity.domain.exception.TenantMembershipInactiveException;
import br.com.f2e.ovenplatform.identity.infrastructure.security.AuthenticatedUserDetails;
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
  private static final UUID USER_ID = UUID.randomUUID();
  private static final String EMAIL = "john@email.com";
  private static final String PASSWORD_HASH = "password-hash";

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
    var user = createUser();
    var userDetails = createUserDetails(user);
    var authenticated =
        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

    when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);
    when(accessTokenService.generateToken(TENANT_ID, USER_ID, TenantMembershipRole.MEMBER.name()))
        .thenReturn("jwt-token");
    when(tenantMembershipRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
        .thenReturn(Optional.of(createTenantMembership(user, TenantMembershipRole.MEMBER)));

    var token = authService.login(TENANT_ID, EMAIL, "123456");

    assertEquals("jwt-token", token);
    assertSame(authenticated, SecurityContextHolder.getContext().getAuthentication());

    verify(accessTokenService)
        .generateToken(TENANT_ID, USER_ID, TenantMembershipRole.MEMBER.name());
  }

  @Test
  void shouldAuthenticateUsingEmailAndPassword() {
    var user = createUser();
    var userDetails = createUserDetails(user);
    var authenticated =
        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

    when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);
    when(accessTokenService.generateToken(TENANT_ID, USER_ID, TenantMembershipRole.ADMIN.name()))
        .thenReturn("jwt-token");
    when(tenantMembershipRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
        .thenReturn(Optional.of(createTenantMembership(user, TenantMembershipRole.ADMIN)));

    authService.login(TENANT_ID, EMAIL, "123456");

    verify(authenticationManager)
        .authenticate(
            argThat(
                authentication ->
                    Objects.equals(authentication.getPrincipal(), EMAIL)
                        && Objects.equals(authentication.getCredentials(), "123456")
                        && !authentication.isAuthenticated()));
  }

  @Test
  void shouldNotGenerateTokenWhenAuthenticationFails() {
    when(authenticationManager.authenticate(any(Authentication.class)))
        .thenThrow(new BadCredentialsException("Bad credentials"));

    assertThrows(
        BadCredentialsException.class, () -> authService.login(TENANT_ID, EMAIL, "wrong-password"));

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verifyNoInteractions(accessTokenService);
  }

  @Test
  void shouldFailWhenAuthenticationPrincipalIsInvalid() {
    var authenticated = new UsernamePasswordAuthenticationToken(EMAIL, null, List.of());

    when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);

    assertThatThrownBy(() -> authService.login(TENANT_ID, EMAIL, "123456"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Authenticated principal is not an AuthenticatedPrincipal");

    verifyNoInteractions(accessTokenService);
  }

  @Test
  void shouldFailWhenTenantMembershipIsInactive() {
    var user = createUser();
    var userDetails = createUserDetails(user);
    var authenticated =
        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

    var tenantMembership = createTenantMembership(user, TenantMembershipRole.ADMIN);
    tenantMembership.deactivate();

    when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);
    when(tenantMembershipRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
        .thenReturn(Optional.of(tenantMembership));

    assertThatThrownBy(() -> authService.login(TENANT_ID, EMAIL, "123456"))
        .isInstanceOf(TenantMembershipInactiveException.class)
        .hasMessage("Tenant membership is inactive.");
  }

  @Test
  void shouldFailWhenUserHasNoMembershipForTenant() {
    var user = createUser();
    var userDetails = createUserDetails(user);
    var authenticated =
        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

    when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);
    when(tenantMembershipRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.login(TENANT_ID, EMAIL, "123456"))
        .isInstanceOf(TenantAccessDeniedException.class)
        .hasMessage("User does not have access to this tenant.");

    verifyNoInteractions(accessTokenService);
  }

  private static User createUser() {
    return withId(new User(EMAIL, PASSWORD_HASH), USER_ID);
  }

  private static AuthenticatedUserDetails createUserDetails(User user) {
    return new AuthenticatedUserDetails(user.getId(), user.getEmail(), user.getPasswordHash());
  }

  private TenantMembership createTenantMembership(User user, TenantMembershipRole role) {
    return new TenantMembership(user, TENANT_ID, role);
  }
}
