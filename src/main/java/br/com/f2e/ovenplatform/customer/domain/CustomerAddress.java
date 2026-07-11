package br.com.f2e.ovenplatform.customer.domain;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import br.com.f2e.ovenplatform.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer_addresses")
public class CustomerAddress extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @Column(length = 40)
  private String label;

  @Column(name = "address_line_1", nullable = false, length = 120)
  private String addressLine1;

  @Column(nullable = false, length = 20)
  private String number;

  @Column(length = 120)
  private String complement;

  @Column(nullable = false, length = 80)
  private String neighborhood;

  @Column(nullable = false, length = 80)
  private String city;

  @Column(nullable = false, length = 40)
  private String state;

  @Column(nullable = false, length = 20)
  private String postalCode;

  @Column(length = 160)
  private String reference;

  protected CustomerAddress() {}

  CustomerAddress(Customer customer, CustomerAddressDetails details) {
    this.customer = requireNotNull(customer, "customer");
    update(details);
  }

  void update(CustomerAddressDetails details) {
    var validDetails = requireNotNull(details, "details");
    var addressLine = validDetails.addressLine();
    var location = validDetails.location();

    this.label = validDetails.label();
    this.addressLine1 = addressLine.addressLine1();
    this.number = addressLine.number();
    this.complement = addressLine.complement();
    this.neighborhood = location.neighborhood();
    this.city = location.city();
    this.state = location.state();
    this.postalCode = location.postalCode();
    this.reference = validDetails.reference();
  }

  public String getLabel() {
    return label;
  }

  public String getAddressLine1() {
    return addressLine1;
  }

  public String getNumber() {
    return number;
  }

  public String getComplement() {
    return complement;
  }

  public String getNeighborhood() {
    return neighborhood;
  }

  public String getCity() {
    return city;
  }

  public String getState() {
    return state;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public String getReference() {
    return reference;
  }
}
