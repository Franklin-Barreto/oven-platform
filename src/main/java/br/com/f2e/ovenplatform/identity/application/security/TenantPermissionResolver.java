package br.com.f2e.ovenplatform.identity.application.security;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotEmptyAndWithoutNulls;

import br.com.f2e.ovenplatform.identity.application.api.security.TenantPermission;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public final class TenantPermissionResolver {

  private static final Map<TenantMembershipRole, Set<TenantPermission>> PERMISSIONS_BY_ROLE =
      createPermissionsByRole();

  public Set<TenantPermission> resolve(Set<TenantMembershipRole> roles) {
    requireNotEmptyAndWithoutNulls(roles, "roles");
    return Set.copyOf(
        roles.stream()
            .map(PERMISSIONS_BY_ROLE::get)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet()));
  }

  private static Map<TenantMembershipRole, Set<TenantPermission>> createPermissionsByRole() {
    Map<TenantMembershipRole, Set<TenantPermission>> map =
        new EnumMap<>(TenantMembershipRole.class);
    mapKitchen(map);
    mapAttendant(map);
    mapManager(map);
    mapOwner(map);
    return Map.copyOf(map);
  }

  private static void mapKitchen(Map<TenantMembershipRole, Set<TenantPermission>> map) {
    map.put(
        TenantMembershipRole.KITCHEN,
        Set.of(TenantPermission.KITCHEN_OPERATE, TenantPermission.KITCHEN_READ));
  }

  private static void mapAttendant(Map<TenantMembershipRole, Set<TenantPermission>> map) {
    map.put(
        TenantMembershipRole.ATTENDANT,
        Set.of(
            TenantPermission.CATALOG_READ,
            TenantPermission.CUSTOMER_READ,
            TenantPermission.CUSTOMER_MANAGE,
            TenantPermission.ORDER_READ,
            TenantPermission.ORDER_CREATE,
            TenantPermission.ORDER_MANAGE,
            TenantPermission.PAYMENT_READ,
            TenantPermission.PAYMENT_MANAGE));
  }

  private static void mapManager(Map<TenantMembershipRole, Set<TenantPermission>> map) {
    map.put(
        TenantMembershipRole.MANAGER,
        Set.of(
            TenantPermission.TEAM_READ,
            TenantPermission.TEAM_MANAGE,
            TenantPermission.CATALOG_READ,
            TenantPermission.CATALOG_MANAGE,
            TenantPermission.CUSTOMER_READ,
            TenantPermission.CUSTOMER_MANAGE,
            TenantPermission.ORDER_READ,
            TenantPermission.ORDER_CREATE,
            TenantPermission.ORDER_MANAGE,
            TenantPermission.PAYMENT_READ,
            TenantPermission.PAYMENT_MANAGE,
            TenantPermission.KITCHEN_READ,
            TenantPermission.KITCHEN_OPERATE));
  }

  private static void mapOwner(Map<TenantMembershipRole, Set<TenantPermission>> map) {
    map.put(
        TenantMembershipRole.OWNER,
        Set.of(
            TenantPermission.TEAM_READ,
            TenantPermission.TEAM_MANAGE,
            TenantPermission.CATALOG_READ,
            TenantPermission.CATALOG_MANAGE,
            TenantPermission.CUSTOMER_READ,
            TenantPermission.CUSTOMER_MANAGE,
            TenantPermission.ORDER_READ,
            TenantPermission.ORDER_CREATE,
            TenantPermission.ORDER_MANAGE,
            TenantPermission.PAYMENT_READ,
            TenantPermission.PAYMENT_MANAGE,
            TenantPermission.KITCHEN_READ,
            TenantPermission.KITCHEN_OPERATE));
  }
}
