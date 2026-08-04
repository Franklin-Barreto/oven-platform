package br.com.f2e.ovenplatform.payment.domain;

public enum ExternalPaymentAttemptStatus {
  CREATED {
    @Override
    boolean canTransitionTo(ExternalPaymentAttemptStatus target) {
      return target == PENDING || target == FAILED;
    }
  },

  PENDING {
    @Override
    boolean canTransitionTo(ExternalPaymentAttemptStatus target) {
      return target == SUCCEEDED || target == FAILED || target == EXPIRED;
    }
  },

  SUCCEEDED {
    @Override
    boolean canTransitionTo(ExternalPaymentAttemptStatus target) {
      return false;
    }
  },

  FAILED {
    @Override
    boolean canTransitionTo(ExternalPaymentAttemptStatus target) {
      return false;
    }
  },

  EXPIRED {
    @Override
    boolean canTransitionTo(ExternalPaymentAttemptStatus target) {
      return false;
    }
  };

  abstract boolean canTransitionTo(ExternalPaymentAttemptStatus target);
}
