package br.com.f2e.ovenplatform.shared.application.event;

public final class OrderEventConstants {

  public static final String AGGREGATE_TYPE = "ORDER";
  public static final String ORDER_CREATED_EVENT = "order.created";
  public static final int PAYLOAD_VERSION = 1;

  private OrderEventConstants() {}
}
