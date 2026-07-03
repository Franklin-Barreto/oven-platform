package br.com.f2e.ovenplatform.kitchen.domain;

public enum TicketStatus {
  RECEIVED {
    @Override
    boolean canTransitionTo(TicketStatus target) {
      return target == IN_PREPARATION || target == CANCELLED;
    }
  },
  IN_PREPARATION {
    @Override
    boolean canTransitionTo(TicketStatus target) {
      return target == READY || target == CANCELLED;
    }
  },
  READY {
    @Override
    boolean canTransitionTo(TicketStatus target) {
      return false;
    }
  },
  CANCELLED {
    @Override
    boolean canTransitionTo(TicketStatus target) {
      return false;
    }
  };

  abstract boolean canTransitionTo(TicketStatus target);
}
