package br.com.f2e.ovenplatform.orders.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.UUID;

@Embeddable
public class DeliveryCustomerSnapshot {

  @Column(name = "customer_id")
  private UUID customerId;

  @Column(name = "customer_name", length = 80)
  private String customerName;

  @Column(name = "customer_phone", length = 30)
  private String customerPhone;

  @Column(name = "delivery_address_id")
  private UUID addressId;

  @Column(name = "delivery_address_label", length = 40)
  private String addressLabel;

  @Column(name = "delivery_address_line_1", length = 120)
  private String addressLine1;

  @Column(name = "delivery_number", length = 20)
  private String number;

  @Column(name = "delivery_complement", length = 120)
  private String complement;

  @Column(name = "delivery_neighborhood", length = 80)
  private String neighborhood;

  @Column(name = "delivery_city", length = 80)
  private String city;

  @Column(name = "delivery_state", length = 40)
  private String state;

  @Column(name = "delivery_postal_code", length = 20)
  private String postalCode;

  @Column(name = "delivery_reference", length = 160)
  private String reference;

  protected DeliveryCustomerSnapshot() {}

  public DeliveryCustomerSnapshot(DeliveryCustomerDetails details) {
    var address = details.address();
    var line = address.line();
    var location = address.location();

    this.customerId = details.customerId();
    this.customerName = details.customerName();
    this.customerPhone = details.customerPhone();
    this.addressId = address.addressId();
    this.addressLabel = address.label();
    this.addressLine1 = line.addressLine1();
    this.number = line.number();
    this.complement = line.complement();
    this.neighborhood = location.neighborhood();
    this.city = location.city();
    this.state = location.state();
    this.postalCode = location.postalCode();
    this.reference = address.reference();
  }

  public UUID getCustomerId() {
    return customerId;
  }

  public String getCustomerName() {
    return customerName;
  }

  public String getCustomerPhone() {
    return customerPhone;
  }

  public DeliveryAddressDetails getAddress() {
    return new DeliveryAddressDetails(
        addressId,
        addressLabel,
        new DeliveryAddressLine(addressLine1, number, complement),
        new DeliveryAddressLocation(neighborhood, city, state, postalCode),
        reference);
  }
}
