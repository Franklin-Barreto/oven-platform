package br.com.f2e.ovenplatform.kitchen.domain.exception;

import br.com.f2e.ovenplatform.kitchen.domain.TicketStatus;

public class InvalidTicketStatusTransitionException extends RuntimeException {

  public InvalidTicketStatusTransitionException(
      TicketStatus currentStatus, TicketStatus targetStatus) {
    super("Cannot transition ticket from %s to %s.".formatted(currentStatus, targetStatus));
  }
}
