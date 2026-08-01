package br.com.f2e.ovenplatform.catalog.domain;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.*;

import br.com.f2e.ovenplatform.shared.domain.BaseEntity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "option_groups")
public class OptionGroup extends BaseEntity {

  @Column(nullable = false)
  private UUID productId;

  @Column(nullable = false)
  private UUID tenantId;

  @Column(nullable = false, length = 80)
  private String name;

  @Column(nullable = false)
  private int minimumSelections;

  @Column(nullable = false)
  private int maximumSelections;

  @Column(nullable = false)
  private int displayPosition;

  @Column(nullable = false)
  private boolean active;

  @SuppressWarnings("unused")
  protected OptionGroup() {}

  @SuppressFBWarnings(
      value = "CT_CONSTRUCTOR_THROW",
      justification = "Domain invariants are validated while constructing the aggregate.")
  public OptionGroup(
      UUID productId,
      UUID tenantId,
      String name,
      int minimumSelections,
      int maximumSelections,
      int displayPosition) {
    this.productId = requireNotNull(productId, "productId");
    this.tenantId = requireNotNull(tenantId, "tenantId");
    this.name = requireSize(name, "name", 1, 80);
    validateSelectionLimits(minimumSelections, maximumSelections);
    this.minimumSelections = minimumSelections;
    this.maximumSelections = maximumSelections;
    this.displayPosition = requireNonNegative(displayPosition, "displayPosition");
    this.active = true;
  }

  public UUID getProductId() {
    return productId;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public String getName() {
    return name;
  }

  public int getMinimumSelections() {
    return minimumSelections;
  }

  public int getMaximumSelections() {
    return maximumSelections;
  }

  public int getDisplayPosition() {
    return displayPosition;
  }

  public boolean isActive() {
    return active;
  }

  public boolean isRequired() {
    return minimumSelections > 0;
  }

  public void updateDetails(String name, int minimumSelections, int maximumSelections) {
    String validatedName = requireSize(name, "name", 1, 80);
    validateSelectionLimits(minimumSelections, maximumSelections);

    this.name = validatedName;
    this.minimumSelections = minimumSelections;
    this.maximumSelections = maximumSelections;
  }

  public void changeDisplayPosition(int displayPosition) {
    this.displayPosition = requireNonNegative(displayPosition, "displayPosition");
  }

  public void changeStatusTo(boolean active) {
    this.active = active;
  }

  private static void validateSelectionLimits(int minimumSelections, int maximumSelections) {
    requireNonNegative(minimumSelections, "minimumSelections");
    requireNonNegative(maximumSelections, "maximumSelections");

    if (maximumSelections < minimumSelections) {
      throw new IllegalArgumentException(
          "maximumSelections must be greater than or equal to minimumSelections");
    }
  }
}
