package br.com.f2e.ovenplatform.payment.infrastructure.web;

import static br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders.API_VERSION_VALUE;

import br.com.f2e.ovenplatform.identity.application.api.security.CurrentTenantId;
import br.com.f2e.ovenplatform.payment.application.OrderPaymentResponse;
import br.com.f2e.ovenplatform.payment.application.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentController {

  private final PaymentService paymentService;

  public PaymentController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @PostMapping(version = API_VERSION_VALUE, path = "/orders/lookup")
  public ResponseEntity<List<OrderPaymentResponse>> findByOrderIds(
      @CurrentTenantId UUID tenantId, @Valid @RequestBody OrderPaymentsLookupRequest request) {

    var responses = paymentService.findByTenantIdAndOrderIdIn(tenantId, request.orderIds());

    return ResponseEntity.ok(responses);
  }
}
