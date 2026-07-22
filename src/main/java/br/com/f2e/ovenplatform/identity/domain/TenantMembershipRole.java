package br.com.f2e.ovenplatform.identity.domain;

public enum TenantMembershipRole {
  MANAGER,
  ATTENDANT,
  OWNER,
  KITCHEN;

  public boolean canManage(TenantMembershipRole targetRole) {
    return switch (this) {
      case OWNER -> targetRole != OWNER;
      case MANAGER -> targetRole == ATTENDANT || targetRole == KITCHEN;
      case ATTENDANT, KITCHEN -> false;
    };
  }
}
