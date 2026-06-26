package br.com.f2e.ovenplatform.identity.application;

import static br.com.f2e.ovenplatform.identity.domain.validation.EmailNormalizer.normalize;

import br.com.f2e.ovenplatform.identity.domain.TenantMembership;
import br.com.f2e.ovenplatform.identity.domain.User;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final TenantMembershipRepository tenantMembershipRepository;
  private final TenantValidator tenantValidator;

  public IdentityService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      TenantMembershipRepository tenantMembershipRepository,
      TenantValidator tenantValidator) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.tenantMembershipRepository = tenantMembershipRepository;
    this.tenantValidator = tenantValidator;
  }

  @Transactional
  public TenantUserResult createTenantUser(CreateTenantUserCommand command) {
    tenantValidator.ensureTenantExists(command.tenantId());

    var email = normalize(command.email());

    var user =
        userRepository
            .findByEmail(email)
            .orElseGet(() -> createUserForTenantMembership(command, email));

    TenantMembership saved =
        tenantMembershipRepository.save(
            new TenantMembership(user, command.tenantId(), command.role()));
    return new TenantUserResult(
        saved.getUser().getId(),
        saved.getTenantId(),
        saved.getUser().getEmail(),
        saved.getRole(),
        saved.getStatus());
  }

  private User createUserForTenantMembership(CreateTenantUserCommand command, String email) {
    var passwordHash = passwordEncoder.encode(command.rawPassword());

    return userRepository.save(new User(command.tenantId(), email, passwordHash));
  }

  @Transactional(readOnly = true)
  public User findByIdAndTenantId(UUID id, UUID tenantId) {
    return userRepository
        .findByIdAndTenantId(id, tenantId)
        .orElseThrow(() -> new NoSuchElementException("User"));
  }

  @Transactional(readOnly = true)
  public TenantUserResult findTenantUserById(UUID tenantId, UUID userId) {
    var membership =
        tenantMembershipRepository
            .findByUserIdAndTenantId(userId, tenantId)
            .orElseThrow(() -> new NoSuchElementException("User"));

    return new TenantUserResult(
        membership.getUser().getId(),
        membership.getTenantId(),
        membership.getUser().getEmail(),
        membership.getRole(),
        membership.getStatus());
  }
}
