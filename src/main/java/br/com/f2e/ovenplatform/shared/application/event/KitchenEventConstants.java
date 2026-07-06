package br.com.f2e.ovenplatform.shared.application.event;

public final class KitchenEventConstants {

  public static final String TOPIC = "kitchen-events";
  public static final String AGGREGATE_TYPE = "KITCHEN_TICKET";
  public static final String TICKET_READY_EVENT = "kitchen.ticket.ready";
  public static final int PAYLOAD_VERSION = 1;

  private KitchenEventConstants() {}
}
