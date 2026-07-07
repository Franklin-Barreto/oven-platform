package br.com.f2e.ovenplatform.shared.application.event;

public final class FulfillmentEventConstants {

  public static final String AGGREGATE_TYPE = "FULFILLMENT_ORDER";
  public static final String FULFILLMENT_ORDER_READY_EVENT = "fulfillment.order.ready";
  public static final int PAYLOAD_VERSION = 1;

  private FulfillmentEventConstants() {}
}
