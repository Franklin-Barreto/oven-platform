package br.com.f2e.ovenplatform.payment.infrastructure.web;

import br.com.f2e.ovenplatform.identity.application.api.security.CurrentTenantId;
import br.com.f2e.ovenplatform.payment.application.OrderPaymentResult;
import br.com.f2e.ovenplatform.payment.application.PaymentService;
import br.com.f2e.ovenplatform.payment.application.checkout.PaymentCheckoutSessionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders.API_VERSION_VALUE;

@RestController
@RequestMapping("/payments")
public class PaymentController {

  private final PaymentService paymentService;
  private final PaymentCheckoutSessionService paymentCheckoutService;

  public PaymentController(
      PaymentService paymentService, PaymentCheckoutSessionService paymentCheckoutService) {
    this.paymentService = paymentService;
    this.paymentCheckoutService = paymentCheckoutService;
  }

  @PreAuthorize("hasAuthority('PAYMENT_READ')")
  @PostMapping(version = API_VERSION_VALUE, path = "/orders/lookup")
  public ResponseEntity<List<OrderPaymentResult>> findByOrderIds(
      @CurrentTenantId UUID tenantId, @Valid @RequestBody OrderPaymentsLookupRequest request) {

    var responses = paymentService.findByTenantIdAndOrderIdIn(tenantId, request.orderIds());

    return ResponseEntity.ok(responses);
  }

  @PreAuthorize("hasAuthority('PAYMENT_MANAGE')")
  @PostMapping(version = API_VERSION_VALUE, path = "/orders/{orderId}/checkout-sessions")
  public ResponseEntity<PaymentCheckoutSessionResponse> createCheckoutSession(
      @CurrentTenantId UUID tenantId, @PathVariable UUID orderId) {

    var response =
        PaymentCheckoutSessionResponse.from(
            paymentCheckoutService.createOrReuseCheckoutSession(tenantId, orderId));
    return ResponseEntity.ok(response);
  }
}
