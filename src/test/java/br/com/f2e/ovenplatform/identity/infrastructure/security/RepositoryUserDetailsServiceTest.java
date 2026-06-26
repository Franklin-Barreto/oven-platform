package br.com.f2e.ovenplatform.identity.infrastructure.security;

import static br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.EntityIdTestUtils.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.identity.application.UserRepository;
import br.com.f2e.ovenplatform.identity.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class RepositoryUserDetailsServiceTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private static final String EMAIL = "john@email.com";
  private static final String PASSWORD_HASH = "password-hash";

  @Mock private UserRepository userRepository;

  private RepositoryUserDetailsService service;

  @BeforeEach
  void setUp() {
    service = new RepositoryUserDetailsService(userRepository);
  }

  @Test
  void shouldLoadUserByUsername() {
    var user = withId(new User(UUID.randomUUID(), EMAIL, PASSWORD_HASH), USER_ID);

    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

    var userDetails = service.loadUserByUsername(EMAIL);

    assertThat(userDetails).isInstanceOf(AuthenticatedUserDetails.class);
    assertThat(userDetails.getUsername()).isEqualTo(EMAIL);
    assertThat(userDetails.getPassword()).isEqualTo(PASSWORD_HASH);
    assertThat(userDetails.getAuthorities()).isEmpty();

    var authenticatedUserDetails = (AuthenticatedUserDetails) userDetails;

    assertThat(authenticatedUserDetails.userId()).isEqualTo(USER_ID);
  }

  @Test
  void shouldThrowWhenUserDoesNotExist() {
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.loadUserByUsername(EMAIL))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessage("User not found");
  }
}
