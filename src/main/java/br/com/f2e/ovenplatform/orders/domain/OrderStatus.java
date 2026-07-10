package br.com.f2e.ovenplatform.orders.domain;

public enum OrderStatus {
  CREATED {
    @Override
    boolean canTransitionTo(OrderStatus target) {
      return target == READY || target == CANCELLED;
    }
  },

  READY {
    @Override
    boolean canTransitionTo(OrderStatus target) {
      return target == COMPLETED;
    }
  },

  COMPLETED {
    @Override
    boolean canTransitionTo(OrderStatus target) {
      return false;
    }
  },

  CANCELLED {
    @Override
    boolean canTransitionTo(OrderStatus target) {
      return false;
    }
  };

  abstract boolean canTransitionTo(OrderStatus target);
}
