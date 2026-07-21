package br.com.f2e.ovenplatform.e2e.support;

import br.com.f2e.ovenplatform.identity.application.TenantMembershipRepository;
import br.com.f2e.ovenplatform.identity.application.UserRepository;
import br.com.f2e.ovenplatform.identity.domain.TenantMembership;
import br.com.f2e.ovenplatform.identity.domain.User;
import br.com.f2e.ovenplatform.tenant.application.TenantRepository;
import br.com.f2e.ovenplatform.tenant.domain.Plan;
import br.com.f2e.ovenplatform.tenant.domain.Tenant;
import io.cucumber.spring.ScenarioScope;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ScenarioScope
public class OwnerUserSeed {

  private static final String OWNER_EMAIL = "owner@oven.test";
  private static final String OWNER_PASSWORD = "1234567";

  private final TenantRepository tenantRepository;
  private final UserRepository userRepository;
  private final TenantMembershipRepository tenantMembershipRepository;
  private final PasswordEncoder passwordEncoder;

  public OwnerUserSeed(
      TenantRepository tenantRepository,
      UserRepository userRepository,
      TenantMembershipRepository tenantMembershipRepository,
      PasswordEncoder passwordEncoder) {
    this.tenantRepository = tenantRepository;
    this.userRepository = userRepository;
    this.tenantMembershipRepository = tenantMembershipRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public SeededOwnerUser seedOwnerForTenant(String tenantName) {
    var tenant = ensureTenant(tenantName);
    var user = ensureUser();

    ensureOwnerMembership(user, tenant);

    return new SeededOwnerUser(tenant.getId(), user.getEmail(), OWNER_PASSWORD);
  }

  private Tenant ensureTenant(String tenantName) {
    return tenantRepository
        .findByName(tenantName)
        .orElseGet(() -> tenantRepository.save(new Tenant(tenantName, Plan.MVP)));
  }

  private User ensureUser() {
    return userRepository
        .findByEmail(OWNER_EMAIL)
        .orElseGet(
            () ->
                userRepository.save(new User(OWNER_EMAIL, passwordEncoder.encode(OWNER_PASSWORD))));
  }

  private void ensureOwnerMembership(User user, Tenant tenant) {
    if (tenantMembershipRepository.existsByUserIdAndTenantId(user.getId(), tenant.getId())) {
      return;
    }

    tenantMembershipRepository.save(TenantMembership.owner(user, tenant.getId()));
  }
}
