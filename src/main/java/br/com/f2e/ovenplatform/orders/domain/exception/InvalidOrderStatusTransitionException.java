package br.com.f2e.ovenplatform.orders.domain.exception;

import br.com.f2e.ovenplatform.orders.domain.OrderStatus;

public class InvalidOrderStatusTransitionException extends RuntimeException {

  public InvalidOrderStatusTransitionException(
      OrderStatus currentStatus, OrderStatus targetStatus) {
    super("Cannot transition order from " + currentStatus + " to " + targetStatus + ".");
  }
}
