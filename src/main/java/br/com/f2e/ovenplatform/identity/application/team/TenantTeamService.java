package br.com.f2e.ovenplatform.identity.application.team;

import static br.com.f2e.ovenplatform.identity.domain.validation.EmailNormalizer.normalize;

import br.com.f2e.ovenplatform.identity.application.port.TenantMembershipRepository;
import br.com.f2e.ovenplatform.identity.application.port.TenantValidator;
import br.com.f2e.ovenplatform.identity.application.port.UserRepository;
import br.com.f2e.ovenplatform.identity.domain.TenantMembership;
import br.com.f2e.ovenplatform.identity.domain.User;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantTeamService {

  private static final String RESOURCE = "User";

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final TenantMembershipRepository tenantMembershipRepository;
  private final TenantValidator tenantValidator;
  private final TenantTeamManagementPolicy managementPolicy;

  public TenantTeamService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      TenantMembershipRepository tenantMembershipRepository,
      TenantValidator tenantValidator,
      TenantTeamManagementPolicy managementPolicy) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.tenantMembershipRepository = tenantMembershipRepository;
    this.tenantValidator = tenantValidator;
    this.managementPolicy = managementPolicy;
  }

  @Transactional
  public TenantUserResult createTenantUser(CreateTenantUserCommand command) {
    tenantValidator.ensureTenantExists(command.tenantId());

    var actorMembership = findMembership(command.actorUserId(), command.tenantId());
    managementPolicy.ensureCanManageMembership(actorMembership.getRoles(), command.roles());

    var email = normalize(command.email());

    var user =
        userRepository
            .findByEmail(email)
            .orElseGet(() -> createUserForTenantMembership(command, email));

    var tenantMembership = TenantMembership.staff(user, command.tenantId(), command.roles());

    TenantMembership saved = tenantMembershipRepository.save(tenantMembership);
    return new TenantUserResult(
        saved.getUser().getId(),
        saved.getTenantId(),
        saved.getUser().getEmail(),
        saved.getRoles(),
        saved.getStatus());
  }

  @Transactional(readOnly = true)
  public TenantUserResult findTenantUserById(UUID tenantId, UUID userId) {
    var membership =
        tenantMembershipRepository
            .findByUserIdAndTenantId(userId, tenantId)
            .orElseThrow(() -> new NoSuchElementException(RESOURCE));

    return new TenantUserResult(
        membership.getUser().getId(),
        membership.getTenantId(),
        membership.getUser().getEmail(),
        membership.getRoles(),
        membership.getStatus());
  }

  @Transactional
  public TenantUserResult replaceTenantMembershipRoles(
      ReplaceTenantMembershipRolesCommand command) {

    var actorMembership =
        tenantMembershipRepository
            .findByUserIdAndTenantId(command.actorUserId(), command.tenantId())
            .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, command.actorUserId()));
    var targetMembership =
        tenantMembershipRepository
            .findByUserIdAndTenantId(command.targetUserId(), command.tenantId())
            .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, command.targetUserId()));
    managementPolicy.ensureCanChangeRoles(
        actorMembership.getRoles(), targetMembership.getRoles(), command.roles());

    targetMembership.changeOperationalRolesTo(command.roles());
    return TenantUserResult.from(targetMembership);
  }

  @Transactional(readOnly = true)
  public List<TenantUserResult> listTenantMemberships(UUID tenantId) {
    return tenantMembershipRepository.findAllByTenantId(tenantId).stream()
        .map(TenantUserResult::from)
        .toList();
  }

  @Transactional
  public void deactivateTenantMembership(DeactivateTenantMembershipCommand command) {

    var actorMembership = findMembership(command.actorUserId(), command.tenantId());
    var targetMembership = findMembership(command.targetUserId(), command.tenantId());

    managementPolicy.ensureCanManageMembership(
        actorMembership.getRoles(), targetMembership.getRoles());

    targetMembership.deactivate();
  }

  @Transactional
  public void reactivateTenantMembership(ReactivateTenantMembershipCommand command) {

    var actorMembership = findMembership(command.actorUserId(), command.tenantId());
    var targetMembership = findMembership(command.targetUserId(), command.tenantId());

    managementPolicy.ensureCanManageMembership(
        actorMembership.getRoles(), targetMembership.getRoles());

    targetMembership.activate();
  }

  private User createUserForTenantMembership(CreateTenantUserCommand command, String email) {
    var passwordHash = passwordEncoder.encode(command.rawPassword());

    return userRepository.save(new User(email, passwordHash));
  }

  private TenantMembership findMembership(UUID userId, UUID tenantId) {
    return tenantMembershipRepository
        .findByUserIdAndTenantId(userId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, userId));
  }
}
