package br.com.f2e.ovenplatform.customer.domain;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.normalizeOptional;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireMinimumSize;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotBlank;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import br.com.f2e.ovenplatform.shared.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {

  private static final String PHONE_FIELD = "phone";

  @Column(nullable = false)
  private UUID tenantId;

  @Column(nullable = false, length = 80)
  private String name;

  @Column(nullable = false, length = 30)
  private String phone;

  @Column(nullable = false, length = 30)
  private String normalizedPhone;

  @Column(length = 500)
  private String notes;

  @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
  private final List<CustomerAddress> addresses = new ArrayList<>();

  protected Customer() {}

  private Customer(UUID tenantId, String name, String phone, String normalizedPhone, String notes) {
    this.tenantId = tenantId;
    this.name = name;
    this.phone = phone;
    this.normalizedPhone = normalizedPhone;
    this.notes = notes;
  }

  public static Customer create(UUID tenantId, String name, String phone, String notes) {
    var requiredPhone = requirePhone(phone);

    return new Customer(
        requireNotNull(tenantId, "tenantId"),
        requireMinimumSize(name, "name", 2),
        requiredPhone,
        normalizeRequiredPhone(requiredPhone),
        normalizeOptional(notes));
  }

  public void update(String name, String phone, String notes) {
    var requiredPhone = requirePhone(phone);

    this.name = requireMinimumSize(name, "name", 2);
    this.phone = requiredPhone;
    this.normalizedPhone = normalizeRequiredPhone(requiredPhone);
    this.notes = normalizeOptional(notes);
  }

  public CustomerAddress addAddress(CustomerAddressDetails details) {
    var address = new CustomerAddress(this, details);
    addresses.add(address);
    return address;
  }

  public void updateAddress(UUID addressId, CustomerAddressDetails details) {
    var address = findAddressOrThrow(addressId);
    address.update(details);
  }

  public void removeAddress(UUID addressId) {
    var address = findAddressOrThrow(addressId);
    addresses.remove(address);
  }

  public static String normalizePhone(String phone) {
    return normalizeRequiredPhone(requirePhone(phone));
  }

  private static String requirePhone(String phone) {
    return requireNotBlank(phone, PHONE_FIELD);
  }

  private static String normalizeRequiredPhone(String phone) {
    var normalized =
        phone
            .chars()
            .filter(Character::isDigit)
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString();

    if (normalized.isBlank()) {
      throw new IllegalArgumentException("phone must contain at least one digit");
    }

    return normalized;
  }

  private CustomerAddress findAddressOrThrow(UUID addressId) {
    requireNotNull(addressId, "addressId");
    return addresses.stream()
        .filter(address -> addressId.equals(address.getId()))
        .findFirst()
        .orElseThrow(
            () ->
                new NoSuchElementException(
                    "CustomerAddress id: %s not found".formatted(addressId)));
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public String getName() {
    return name;
  }

  public String getPhone() {
    return phone;
  }

  public String getNormalizedPhone() {
    return normalizedPhone;
  }

  public String getNotes() {
    return notes;
  }

  public List<CustomerAddress> getAddresses() {
    return Collections.unmodifiableList(addresses);
  }
}
